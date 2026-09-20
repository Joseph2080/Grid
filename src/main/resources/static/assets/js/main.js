/**
 * Grid Landing Page - Main JavaScript
 * Handles animations and interactive elements
 */

// ================================
// DOM Content Loaded Event
// ================================
document.addEventListener('DOMContentLoaded', function() {
    // Initialize all functionality
    initMobileMenu();
    initHeaderScroll();
    initLogoFallback();
    initTypewriterEffects();
});

// Re-run typewriter after translations update on language change
document.addEventListener('content:updated', function() {
    initTypewriterEffects();
});

// ================================
// Mobile Menu Toggle
// ================================
function initMobileMenu() {
    const mobileMenuBtn = document.getElementById('mobile-menu-btn');
    const mobileMenu = document.getElementById('mobile-menu');
    const header = document.getElementById('header');
    
    if (mobileMenuBtn && mobileMenu) {
        mobileMenuBtn.addEventListener('click', function() {
            mobileMenu.classList.toggle('active');
            
            // Update ARIA attribute for accessibility
            const isExpanded = mobileMenu.classList.contains('active');
            mobileMenuBtn.setAttribute('aria-expanded', isExpanded);
            
            // Add/remove class to header for background styling
            if (isExpanded) {
                header.classList.add('mobile-menu-open');
            } else {
                header.classList.remove('mobile-menu-open');
            }
        });
        
        // Close mobile menu when clicking on a link
        const mobileLinks = mobileMenu.querySelectorAll('a');
        mobileLinks.forEach(link => {
            link.addEventListener('click', function() {
                mobileMenu.classList.remove('active');
                mobileMenuBtn.setAttribute('aria-expanded', 'false');
                header.classList.remove('mobile-menu-open');
            });
        });
    }
}

// ================================
// Header Scroll Effect
// ================================
function initHeaderScroll() {
    // Header stays transparent on scroll
    // Removed background change on scroll to keep header transparent
}

// ================================
// Logo Image Fallback
// ================================
function initLogoFallback() {
    const logoImg = document.getElementById('logo-img');
    
    if (logoImg) {
        logoImg.addEventListener('error', function() {
            // Hide image if it fails to load
            this.classList.add('hidden');
        });
    }
}

// ================================
// Futuristic Typewriter Effect
// ================================
let typewriterObserver;

function initTypewriterEffects() {
    const targets = document.querySelectorAll('[data-typewriter]');
    if (!targets.length) return;

    if (typewriterObserver) {
        typewriterObserver.disconnect();
    }

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    targets.forEach((target) => {
        const fullText = target.textContent.trim();
        target.dataset.typewriterText = fullText;
        target.classList.remove('typewriter-active', 'typewriter-done');

        if (reduceMotion) {
            target.textContent = fullText;
            target.classList.add('typewriter-done');
        } else {
            target.textContent = '';
        }
    });

    if (reduceMotion) return;

    typewriterObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                animateTypewriter(entry.target);
                observer.unobserve(entry.target);
            }
        });
    }, {
        threshold: 0.35,
        rootMargin: '0px 0px -8% 0px'
    });

    targets.forEach((target) => {
        typewriterObserver.observe(target);
    });
}

function animateTypewriter(element) {
    const text = element.dataset.typewriterText || '';
    let index = 0;

    element.classList.add('typewriter-active');

    function typeNext() {
        if (index <= text.length) {
            element.textContent = text.slice(0, index);
            index += 1;

            const isWhitespace = text.charAt(index - 1) === ' ';
            const delay = isWhitespace ? 65 : 95;
            setTimeout(typeNext, delay);
            return;
        }

        element.classList.remove('typewriter-active');
        element.classList.add('typewriter-done');
    }

    typeNext();
}






// ================================
// Keyboard Accessibility Enhancements
// ================================
document.addEventListener('keydown', function(e) {
    // ESC key closes mobile menu
    if (e.key === 'Escape') {
        const mobileMenu = document.getElementById('mobile-menu');
        const mobileMenuBtn = document.getElementById('mobile-menu-btn');
        
        if (mobileMenu && mobileMenu.classList.contains('active')) {
            mobileMenu.classList.remove('active');
            mobileMenuBtn.setAttribute('aria-expanded', 'false');
        }
    }
});

// ================================
// Page Load Performance Optimization
// ================================
window.addEventListener('load', function() {
    // Mark page as fully loaded
    document.body.classList.add('page-loaded');
    
    // Lazy load images if needed
    const lazyImages = document.querySelectorAll('img[data-src]');
    lazyImages.forEach(img => {
        img.src = img.dataset.src;
        img.removeAttribute('data-src');
    });
});

// ================================
// Error Handling for Images
// ================================
document.addEventListener('error', function(e) {
    if (e.target.tagName === 'IMG') {
        console.warn('Image failed to load:', e.target.src);
        // Could set a placeholder image here
    }
}, true);

// ================================
// Utility Functions
// ================================

/**
 * Debounce function to limit function calls
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Throttle function to limit function calls
 */
function throttle(func, limit) {
    let inThrottle;
    return function(...args) {
        if (!inThrottle) {
            func.apply(this, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

// ================================
// Console Welcome Message (Optional)
// ================================
console.log('%cWelcome to Grid!', 'font-size: 20px; font-weight: bold; color: #000000;');
console.log('%cBuilding the Digital Economy of Tomorrow', 'font-size: 14px; color: #4A4A4A;');
console.log('%cInterested in joining our team? Contact us!', 'font-size: 12px; color: #4A4A4A;');
