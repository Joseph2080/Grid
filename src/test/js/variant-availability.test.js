'use strict';

// Dependency-free checks run with the existing Node runtime:
//   node src\test\js\variant-availability.test.js
//
// Regression coverage for the product-card variant/attribute matching logic:
// a color (or any attribute) that only exists on one size (or any other
// attribute) must be visually flagged as unavailable for the current
// selection once a selection narrows the variant matrix to rows that don't
// include it — but must stay clickable/selectable (soft-disable, not a hard
// `disabled` attribute), and picking it must relax the other attribute(s) to
// a variant that actually exists. This is required for non-rectangular
// variant matrices (e.g. L/S only in black, M only in red) where hard-
// disabling would otherwise trap the user with no way to ever reach M or
// red. This holds regardless of whether the underlying variants/keys use
// different casing, whitespace or key spelling (e.g. a different store's
// attribute schema).
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const templatePath = path.resolve(__dirname, '..', '..', 'main', 'resources', 'templates', 'index.html');
const html = fs.readFileSync(templatePath, 'utf8');

// Extracts the exact, currently-shipped source between two literal markers
// so this test exercises the real index.html code, not a hand-copied
// re-implementation that could silently drift from production behaviour.
function extractBetween(source, startMarker, endMarker) {
  const start = source.indexOf(startMarker);
  assert.ok(start !== -1, `start marker not found in index.html: ${startMarker}`);
  const end = source.indexOf(endMarker, start);
  assert.ok(end !== -1, `end marker not found in index.html: ${endMarker}`);
  return source.slice(start, end);
}

const escapeHtmlSource = extractBetween(
  html,
  'function escapeHtml(value) {',
  '// Returns product media sorted by primary-first'
);

const attributeMatchingSource = extractBetween(
  html,
  'function getProductAttributes(product) {',
  '// Builds the canonical shareable URL for a product.'
);

const sandbox = {};
vm.createContext(sandbox);
vm.runInContext(escapeHtmlSource, sandbox, { filename: 'index.html#escapeHtml' });
vm.runInContext(`var state = {};\n${attributeMatchingSource}`, sandbox, { filename: 'index.html#attribute-matching' });

// Pulls out the <button ...>...</button> markup for a single swatch value
// from a rendered color-attribute control, so assertions can target one
// swatch's classes/attributes without matching across sibling swatches.
function swatchFragment(renderedHtml, value) {
  const marker = `data-attribute-value="${value}"`;
  const markerIndex = renderedHtml.indexOf(marker);
  assert.ok(markerIndex !== -1, `swatch for "${value}" not found in rendered HTML`);
  const start = renderedHtml.lastIndexOf('<button', markerIndex);
  const end = renderedHtml.indexOf('</button>', markerIndex) + '</button>'.length;
  return renderedHtml.slice(start, end);
}

// Pulls out the <option ...>...</option> markup for a single value from a
// rendered generic (non-color) attribute <select>.
function optionFragment(renderedHtml, value) {
  const marker = `value="${value}"`;
  const markerIndex = renderedHtml.indexOf(marker);
  assert.ok(markerIndex !== -1, `option for "${value}" not found in rendered HTML`);
  const start = renderedHtml.lastIndexOf('<option', markerIndex);
  const end = renderedHtml.indexOf('</option>', markerIndex) + '</option>'.length;
  return renderedHtml.slice(start, end);
}

async function run() {
  let checks = 0;
  const check = (name, test) => {
    test();
    checks++;
    console.log(`PASS ${name}`);
  };

  check('a color only present on another size is soft-disabled (clickable, struck through) not hard-disabled', () => {
    // Exactly the reported scenario: size L only ever pairs with black,
    // size M only ever pairs with pink.
    const product = {
      variants: [
        { id: 'v-l-black', stockQuantity: 5, attributes: { size: 'L', colour: 'black' } },
        { id: 'v-m-pink', stockQuantity: 5, attributes: { size: 'M', colour: 'pink' } }
      ],
      attributes: [
        { name: 'size', values: ['L', 'M'], selectable: true, type: 'SELECT' },
        { name: 'colour', values: ['black', 'pink'], selectable: true, type: 'COLOUR' }
      ]
    };
    const colourAttr = product.attributes[1];

    const htmlForL = sandbox.renderAttributeControl(colourAttr, { size: 'L', colour: 'black' }, product.variants);
    const blackFragL = swatchFragment(htmlForL, 'black');
    const pinkFragL = swatchFragment(htmlForL, 'pink');
    assert.doesNotMatch(blackFragL, /disabled/);
    assert.doesNotMatch(blackFragL, /swatch-unavailable/);
    // Unavailable-for-current-selection must stay clickable (no `disabled`
    // attribute), only visually flagged via opacity/the swatch-unavailable marker.
    assert.doesNotMatch(pinkFragL, /disabled/);
    assert.match(pinkFragL, /opacity-30/);
    assert.match(pinkFragL, /swatch-unavailable/);

    const htmlForM = sandbox.renderAttributeControl(colourAttr, { size: 'M', colour: 'pink' }, product.variants);
    const blackFragM = swatchFragment(htmlForM, 'black');
    const pinkFragM = swatchFragment(htmlForM, 'pink');
    assert.doesNotMatch(blackFragM, /disabled/);
    assert.match(blackFragM, /swatch-unavailable/);
    assert.doesNotMatch(pinkFragM, /disabled/);
  });

  check('a size that only pairs with the unselected color stays selectable (not `disabled`) in the size <select>', () => {
    const product = {
      variants: [
        { id: 'v-l-black', stockQuantity: 5, attributes: { size: 'L', colour: 'black' } },
        { id: 'v-m-pink', stockQuantity: 5, attributes: { size: 'M', colour: 'pink' } }
      ],
      attributes: [
        { name: 'size', values: ['L', 'M'], selectable: true, type: 'SELECT' },
        { name: 'colour', values: ['black', 'pink'], selectable: true, type: 'COLOUR' }
      ]
    };
    const sizeAttr = product.attributes[0];

    const renderedHtml = sandbox.renderAttributeControl(sizeAttr, { size: 'L', colour: 'black' }, product.variants);
    assert.match(optionFragment(renderedHtml, 'L'), /selected/);
    assert.doesNotMatch(optionFragment(renderedHtml, 'L'), /disabled/);
    assert.doesNotMatch(optionFragment(renderedHtml, 'M'), /disabled/);
    assert.match(optionFragment(renderedHtml, 'M'), /Unavailable with current selection/);
    // The dropdown must have a visible affordance so it doesn't read as a
    // static/read-only box.
    assert.match(renderedHtml, /<svg[^>]*>[\s\S]*polyline[\s\S]*<\/svg>/);
  });

  check('picking a value with no exact match relaxes the other attribute instead of trapping the user (dead-end regression)', () => {
    // Diagonal matrix: L/S only exist in black, M only exists in red.
    // Starting from the default L+black selection, neither "colour=red" nor
    // "size=M" has a variant matching the *other* currently-selected
    // attribute, so a naive exact-match lookup would leave the user stuck.
    const product = {
      variants: [
        { id: 'v-l-black', stockQuantity: 99, attributes: { size: 'L', colour: '#000000' } },
        { id: 'v-s-black', stockQuantity: 99, attributes: { size: 'S', colour: '#000000' } },
        { id: 'v-m-red', stockQuantity: 99, attributes: { size: 'M', colour: '#FF0000' } }
      ],
      attributes: [
        { name: 'size', values: ['L', 'S', 'M'], selectable: true, type: 'SELECT' },
        { name: 'colour', values: ['#000000', '#FF0000'], selectable: true, type: 'COLOUR' }
      ]
    };
    sandbox.state.selectedProduct = product;

    // User starts on the default L+black variant.
    sandbox.state.selectedAttributes = { size: 'L', colour: '#000000' };

    // User picks size=M (only ever paired with red, not the currently
    // selected black). The colour selection must relax to red so the
    // variant is actually reachable.
    sandbox.state.selectedAttributes = { ...sandbox.state.selectedAttributes, size: 'M' };
    sandbox.updateSelectedVariantFromAttributes('size');
    assert.equal(sandbox.state.selectedVariant.id, 'v-m-red');
    assert.equal(sandbox.state.selectedAttributes.colour, '#FF0000');

    // Reset back to the default and instead pick colour=red directly
    // (only ever paired with M, not the currently selected L). The size
    // selection must relax to M.
    sandbox.state.selectedAttributes = { size: 'L', colour: '#000000' };
    sandbox.state.selectedAttributes = { ...sandbox.state.selectedAttributes, colour: '#FF0000' };
    sandbox.updateSelectedVariantFromAttributes('colour');
    assert.equal(sandbox.state.selectedVariant.id, 'v-m-red');
    assert.equal(sandbox.state.selectedAttributes.size, 'M');
  });

  check('attribute matching is robust to a differently-cased/spelled schema (e.g. a different store)', () => {
    // Simulates a different store's data shape: attribute names/keys and
    // values use different casing/whitespace/key spelling than the
    // canonical attribute list, which real-world imports can introduce.
    const product = {
      variants: [
        { id: 'v-l-black', stockQuantity: 5, attributes: { ' Size ': ' L ', COLOUR: 'BLACK' } },
        { id: 'v-m-pink', stockQuantity: 5, attributes: { size: 'm', colour: ' Pink ' } }
      ],
      attributes: [
        { name: 'Size', values: ['L', 'M'], selectable: true, type: 'SELECT' },
        { name: 'Colour', values: ['Black', 'Pink'], selectable: true, type: 'COLOUR' }
      ]
    };
    const colourAttr = product.attributes[1];

    const selection = { Size: 'L', Colour: 'Black' };
    const htmlForL = sandbox.renderAttributeControl(colourAttr, selection, product.variants);
    const blackFrag = swatchFragment(htmlForL, 'Black');
    const pinkFrag = swatchFragment(htmlForL, 'Pink');
    assert.doesNotMatch(blackFrag, /disabled/);
    assert.match(blackFrag, /swatch-selected/);
    assert.doesNotMatch(pinkFrag, /disabled/);
    assert.match(pinkFrag, /swatch-unavailable/);

    // findMatchingVariant must resolve the differently-cased/spaced variant
    // for the canonically-cased selection built from the attribute list.
    const matched = sandbox.findMatchingVariant(product, selection);
    assert.equal(matched.id, 'v-l-black');
  });

  console.log(`${checks} variant-availability checks passed.`);
}

run().catch(error => { console.error(error); process.exitCode = 1; });
