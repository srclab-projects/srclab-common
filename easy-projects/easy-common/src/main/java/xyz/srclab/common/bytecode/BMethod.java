package xyz.srclab.common.bytecode;

import org.apache.commons.collections4.CollectionUtils;
import xyz.srclab.annotation.Immutable;
import xyz.srclab.annotation.Nullable;
import xyz.srclab.common.collection.iterable.IterableHelper;
import xyz.srclab.common.collection.list.ListHelper;
import xyz.srclab.common.util.string.StringHelper;

import java.util.Collections;
import java.util.List;

/**
 * @author sunqian
 */
@Immutable
public class BMethod implements BDescribable {

    private final String name;
    private final BDescribable returnType;
    private final @Immutable List<BDescribable> parameterTypes;
    private final @Immutable List<BTypeVariable> typeVariables;

    private @Nullable String descriptor;
    private @Nullable String signature;

    public BMethod(
            String name,
            @Nullable BDescribable returnType,
            @Nullable Iterable<BDescribable> parameterTypes,
            @Nullable Iterable<BTypeVariable> typeVariables
    ) {
        this.name = name;
        this.returnType = returnType == null ? ByteCodeHelper.PRIMITIVE_VOID : returnType;
        this.parameterTypes = parameterTypes == null ? Collections.emptyList() :
                ListHelper.immutable(IterableHelper.asList(parameterTypes));
        this.typeVariables = typeVariables == null ? Collections.emptyList() :
                ListHelper.immutable(IterableHelper.asList(typeVariables));
    }

    public String getName() {
        return name;
    }

    public BDescribable getReturnType() {
        return returnType;
    }

    public List<BDescribable> getParameterTypes() {
        return parameterTypes;
    }

    public List<BTypeVariable> getTypeVariables() {
        return typeVariables;
    }

    @Override
    public String getDescriptor() {
        if (descriptor == null) {
            descriptor = getDescriptor0();
        }
        return descriptor;
    }

    private String getDescriptor0() {
        if (CollectionUtils.isEmpty(parameterTypes)) {
            return "()" + returnType.getDescriptor();
        }
        String parameterTypesDescriptor = StringHelper.join("", parameterTypes, BDescribable::getDescriptor);
        return "(" + parameterTypesDescriptor + ")" + returnType.getDescriptor();
    }

    @Override
    public String getSignature() {
        if (signature == null) {
            signature = getSignature0();
        }
        return signature;
    }

    private String getSignature0() {
        String typeVariablesDeclaration = CollectionUtils.isEmpty(typeVariables) ? "" :
                ("<" + StringHelper.join("", typeVariables, BTypeVariable::getDeclaration) + ">");
        String parameterTypesSignature = CollectionUtils.isEmpty(parameterTypes) ? "" :
                StringHelper.join("", parameterTypes, BDescribable::getSignature);
        return typeVariablesDeclaration + "(" + parameterTypesSignature + ")" + returnType.getSignature();
    }
}
