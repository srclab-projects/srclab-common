package xyz.srclab.common.bytecode;

import org.apache.commons.lang3.StringUtils;
import xyz.srclab.annotation.Immutable;

/**
 * @author sunqian
 */
@Immutable
public class BArrayType implements BDescribable {

    private final BDescribable componentType;
    private final String prefix;

    public BArrayType(BDescribable componentType, int dimension) {
        this.componentType = componentType;
        this.prefix = StringUtils.repeat('[', dimension);
    }

    @Override
    public String getDescriptor() {
        return prefix + componentType.getDescriptor();
    }

    @Override
    public String getSignature() {
        return prefix + componentType.getSignature();
    }
}
