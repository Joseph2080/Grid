package org.bazar.vektrlabs.exception;
import lombok.experimental.StandardException;
import org.jericho.common.exception.EntityNotFoundException;

@StandardException
public class AttributeSchemaNotFoundException extends EntityNotFoundException {
}