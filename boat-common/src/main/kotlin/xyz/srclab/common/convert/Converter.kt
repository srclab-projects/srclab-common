package xyz.srclab.common.convert

import xyz.srclab.common.base.*
import xyz.srclab.common.bean.BeanResolver
import xyz.srclab.common.collection.BaseIterableOps.Companion.toAnyArray
import xyz.srclab.common.collection.arrayAsList
import xyz.srclab.common.collection.componentType
import xyz.srclab.common.collection.resolveIterableSchemaOrNull
import xyz.srclab.common.reflect.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal
import java.time.temporal.TemporalAdjuster
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet

interface Converter {

    fun <T> convert(from: Any?, toType: Class<T>): T

    fun <T> convert(from: Any?, toType: Type): T

    @JvmDefault
    fun <T> convert(from: Any?, toTypeRef: TypeRef<T>): T {
        return convert(from, toTypeRef.type)
    }

    fun <T> convert(from: Any?, fromType: Type, toType: Type): T

    @JvmDefault
    fun <T> convert(from: Any?, fromTypeRef: TypeRef<T>, toTypeRef: TypeRef<T>): T {
        return convert(from, fromTypeRef.type, toTypeRef.type)
    }

    fun withPreConvertHandler(preConvertHandler: ConvertHandler): Converter

    companion object {

        @JvmField
        val EMPTY: Converter = object : Converter {

            override fun <T> convert(from: Any?, toType: Class<T>): T {
                throw UnsupportedOperationException("This is an empty converter.")
            }

            override fun <T> convert(from: Any?, toType: Type): T {
                throw UnsupportedOperationException("This is an empty converter.")
            }

            override fun <T> convert(from: Any?, fromType: Type, toType: Type): T {
                throw UnsupportedOperationException("This is an empty converter.")
            }

            override fun withPreConvertHandler(preConvertHandler: ConvertHandler): Converter {
                return ConverterImpl(listOf(preConvertHandler))
            }
        }

        @JvmField
        val NOP: Converter = newConverter(NopConvertHandler)

        @JvmField
        val DEFAULT: Converter = newConverter(ConvertHandler.DEFAULTS)

        @JvmStatic
        fun newConverter(convertHandler: ConvertHandler): Converter {
            return newConverter(listOf(convertHandler))
        }

        @JvmStatic
        fun newConverter(convertHandlers: Iterable<ConvertHandler>): Converter {
            return ConverterImpl(convertHandlers.toList())
        }

        private class ConverterImpl(
            private val handlers: List<ConvertHandler>
        ) : Converter {

            override fun <T> convert(from: Any?, toType: Class<T>): T {
                for (handler in handlers) {
                    val result = handler.convert(from, toType, this)
                    if (result === NULL_VALUE) {
                        return null as T
                    }
                    if (result !== null) {
                        return result.asAny()
                    }
                }
                throw UnsupportedOperationException("Cannot convert $from to $toType.")
            }

            override fun <T> convert(from: Any?, toType: Type): T {
                for (handler in handlers) {
                    val result = handler.convert(from, toType, this)
                    if (result === NULL_VALUE) {
                        return null as T
                    }
                    if (result !== null) {
                        return result.asAny()
                    }
                }
                throw UnsupportedOperationException("Cannot convert $from to $toType.")
            }

            override fun <T> convert(from: Any?, fromType: Type, toType: Type): T {
                for (handler in handlers) {
                    val result = handler.convert(from, fromType, toType, this)
                    if (result === NULL_VALUE) {
                        return null as T
                    }
                    if (result !== null) {
                        return result.asAny()
                    }
                }
                throw UnsupportedOperationException("Cannot convert $fromType to $toType.")
            }

            override fun withPreConvertHandler(preConvertHandler: ConvertHandler): Converter {
                return ConverterImpl(listOf(preConvertHandler).plus(handlers))
            }
        }
    }
}

interface ConvertHandler {

    /**
     * Return null if [from] cannot be converted, return [NULL_VALUE] if result value is null.
     */
    fun convert(from: Any?, toType: Class<*>, converter: Converter): Any?

    /**
     * Return null if [from] cannot be converted, return [NULL_VALUE] if result value is null.
     */
    fun convert(from: Any?, toType: Type, converter: Converter): Any?

    /**
     * Return null if [from] cannot be converted, return [NULL_VALUE] if result value is null.
     */
    fun convert(from: Any?, fromType: Type, toType: Type, converter: Converter): Any?

    companion object {

        @JvmField
        val DEFAULTS: List<ConvertHandler> = listOf(
            NopConvertHandler,
            CharsConvertHandler,
            NumberAndPrimitiveConvertHandler,
            DateTimeConvertHandler.DEFAULT,
            UpperBoundConvertHandler,
            IterableConvertHandler,
            BeanConvertHandler.DEFAULT,
        )

        @JvmStatic
        fun concat(convertHandlers: Iterable<ConvertHandler>): ConvertHandler {
            return object : ConvertHandler {

                override fun convert(from: Any?, toType: Class<*>, converter: Converter): Any? {
                    for (convertHandler in convertHandlers) {
                        val result = convertHandler.convert(from, toType, converter)
                        if (result === NULL_VALUE) {
                            return null
                        }
                        if (result !== null) {
                            return result
                        }
                    }
                    return null
                }

                override fun convert(from: Any?, toType: Type, converter: Converter): Any? {
                    for (convertHandler in convertHandlers) {
                        val result = convertHandler.convert(from, toType, converter)
                        if (result === NULL_VALUE) {
                            return null
                        }
                        if (result !== null) {
                            return result
                        }
                    }
                    return null
                }

                override fun convert(from: Any?, fromType: Type, toType: Type, converter: Converter): Any? {
                    for (convertHandler in convertHandlers) {
                        val result = convertHandler.convert(from, fromType, toType, converter)
                        if (result === NULL_VALUE) {
                            return null
                        }
                        if (result !== null) {
                            return result
                        }
                    }
                    return null
                }
            }
        }
    }
}

abstract class AbstractConvertHandler : ConvertHandler {

    override fun convert(from: Any?, toType: Class<*>, converter: Converter): Any? {
        if (from === null) {
            return doConvertNull(toType, converter)
        }
        return doConvertNotNull(from, from.javaClass, toType, converter)
    }

    override fun convert(from: Any?, toType: Type, converter: Converter): Any? {
        if (from === null) {
            return doConvertNull(toType, converter)
        }
        return doConvertNotNull(from, from.javaClass, toType, converter)
    }

    override fun convert(from: Any?, fromType: Type, toType: Type, converter: Converter): Any? {
        if (from === null) {
            return doConvertNull(toType, converter)
        }
        return doConvertNotNull(from, fromType, toType, converter)
    }

    protected abstract fun doConvertNull(toType: Type, converter: Converter): Any?
    protected abstract fun doConvertNotNull(from: Any, fromType: Type, toType: Type, converter: Converter): Any?
}

object NopConvertHandler : AbstractConvertHandler() {

    override fun doConvertNull(toType: Type, converter: Converter): Any? {
        return null
    }

    override fun doConvertNotNull(from: Any, fromType: Type, toType: Type, converter: Converter): Any? {
        return when {
            fromType == toType -> from
            fromType is Class<*> && toType is Class<*> && toType.isAssignableFrom(fromType) -> from
            else -> null
        }
    }
}

object CharsConvertHandler : AbstractConvertHandler() {

    override fun doConvertNull(toType: Type, converter: Converter): Any? {
        return NULL_VALUE
    }

    override fun doConvertNotNull(from: Any, fromType: Type, toType: Type, converter: Converter): Any? {
        return when (toType) {
            String::class.java, CharSequence::class.java -> {
                if (from is ByteArray) from.toChars() else from.toString()
            }
            StringBuilder::class.java -> {
                if (from is ByteArray)
                    StringBuilder(from.toChars())
                else
                    StringBuilder(from.toString())
            }
            StringBuffer::class.java -> {
                if (from is ByteArray)
                    StringBuffer(from.toChars())
                else
                    StringBuffer(from.toString())
            }
            CharArray::class.java -> {
                if (from is ByteArray)
                    from.toChars().toCharArray()
                else
                    from.toString().toCharArray()
            }
            ByteArray::class.java -> {
                if (from is CharSequence)
                    from.toBytes()
                else
                    null
            }
            else -> null
        }
    }
}

object NumberAndPrimitiveConvertHandler : AbstractConvertHandler() {

    override fun doConvertNull(toType: Type, converter: Converter): Any? {
        return when (toType) {
            Boolean::class.javaPrimitiveType, JavaBoolean::class.java -> null.toBoolean()
            Byte::class.javaPrimitiveType, JavaByte::class.java -> null.toByte()
            Short::class.javaPrimitiveType, JavaShort::class.java -> null.toShort()
            Char::class.javaPrimitiveType, JavaChar::class.java -> null.toChar()
            Int::class.javaPrimitiveType, JavaInt::class.java -> null.toInt()
            Long::class.javaPrimitiveType, JavaLong::class.java -> null.toLong()
            Float::class.javaPrimitiveType, JavaFloat::class.java -> null.toFloat()
            Double::class.javaPrimitiveType, JavaDouble::class.java -> null.toDouble()
            BigInteger::class.java -> null.toBigInteger()
            BigDecimal::class.java -> null.toBigDecimal()
            Number::class.java -> null.toDouble()
            else -> null
        }
    }

    override fun doConvertNotNull(from: Any, fromType: Type, toType: Type, converter: Converter): Any? {
        return when (toType) {
            Boolean::class.javaPrimitiveType, JavaBoolean::class.java -> from.toBoolean()
            Byte::class.javaPrimitiveType, JavaByte::class.java -> from.toByte()
            Short::class.javaPrimitiveType, JavaShort::class.java -> from.toShort()
            Char::class.javaPrimitiveType, JavaChar::class.java -> from.toChar()
            Int::class.javaPrimitiveType, JavaInt::class.java -> from.toInt()
            Long::class.javaPrimitiveType, JavaLong::class.java -> from.toLong()
            Float::class.javaPrimitiveType, JavaFloat::class.java -> from.toFloat()
            Double::class.javaPrimitiveType, JavaDouble::class.java -> from.toDouble()
            BigInteger::class.java -> from.toBigInteger()
            BigDecimal::class.java -> from.toBigDecimal()
            Number::class.java -> from.toDouble()
            else -> null
        }
    }
}

open class DateTimeConvertHandler(
    protected val dateFormat: DateFormat,
    protected val instantFormatter: DateTimeFormatter,
    protected val localDateTimeFormatter: DateTimeFormatter,
    protected val zonedDateTimeFormatter: DateTimeFormatter,
    protected val offsetDateTimeFormatter: DateTimeFormatter,
    protected val localDateFormatter: DateTimeFormatter,
    protected val localTimeFormatter: DateTimeFormatter,
) : AbstractConvertHandler() {

    constructor() : this(
        dateFormat(),
        DateTimeFormatter.ISO_INSTANT,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_ZONED_DATE_TIME,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ISO_LOCAL_TIME
    )

    override fun doConvertNull(toType: Type, converter: Converter): Any? {
        return when (toType) {
            Date::class.java -> null.toDate(dateFormat)
            Instant::class.java -> null.toInstant(instantFormatter)
            LocalDateTime::class.java -> null.toLocalDateTime(localDateTimeFormatter)
            ZonedDateTime::class.java -> null.toZonedDateTime(zonedDateTimeFormatter)
            OffsetDateTime::class.java -> null.toOffsetDateTime(offsetDateTimeFormatter)
            LocalDate::class.java -> null.toLocalDate(localDateFormatter)
            LocalTime::class.java -> null.toLocalTime(localTimeFormatter)
            Duration::class.java -> null.toDuration()
            Temporal::class.java, TemporalAdjuster::class.java -> null.toLocalDateTime(localDateTimeFormatter)
            else -> null
        }
    }

    override fun doConvertNotNull(from: Any, fromType: Type, toType: Type, converter: Converter): Any? {
        return when (toType) {
            Date::class.java -> from.toDate(dateFormat)
            Instant::class.java -> from.toInstant(instantFormatter)
            LocalDateTime::class.java -> from.toLocalDateTime(localDateTimeFormatter)
            ZonedDateTime::class.java -> from.toZonedDateTime(zonedDateTimeFormatter)
            OffsetDateTime::class.java -> from.toOffsetDateTime(offsetDateTimeFormatter)
            LocalDate::class.java -> from.toLocalDate(localDateFormatter)
            LocalTime::class.java -> from.toLocalTime(localTimeFormatter)
            Duration::class.java -> from.toDuration()
            Temporal::class.java, TemporalAdjuster::class.java -> from.toLocalDateTime(localDateTimeFormatter)
            else -> null
        }
    }

    companion object {

        @JvmField
        val DEFAULT: DateTimeConvertHandler = DateTimeConvertHandler()
    }
}

object UpperBoundConvertHandler : AbstractConvertHandler() {

    override fun doConvertNull(toType: Type, converter: Converter): Any? {
        val upperToType = toType.deepUpperBound
        if (upperToType == toType) {
            return null
        }
        return converter.convert(null, upperToType)
    }

    override fun doConvertNotNull(from: Any, fromType: Type, toType: Type, converter: Converter): Any? {
        val upperFromType = fromType.deepUpperBound
        val upperToType = toType.deepUpperBound
        if (upperFromType == fromType && upperToType == toType) {
            return null
        }
        return converter.convert(from, upperFromType, upperToType)
    }
}

object IterableConvertHandler : AbstractConvertHandler() {

    override fun doConvertNull(toType: Type, converter: Converter): Any? {
        return NULL_VALUE
    }

    override fun doConvertNotNull(from: Any, fromType: Type, toType: Type, converter: Converter): Any? {
        if (from is Iterable<*>) {
            return iterableToType(from as Iterable<Any?>, toType, converter)
        }
        val fromClass = from.javaClass
        if (fromClass.isArray) {
            return iterableToType(from.arrayAsList(), toType, converter)
        }
        return null
    }

    private fun iterableToType(iterable: Iterable<Any?>, toType: Type, converter: Converter): Any? {
        val toComponentType = toType.componentType
        if (toComponentType !== null) {
            val upperComponentType = toComponentType.deepUpperBound
            return iterable
                .map { converter.convert<Any?>(it, upperComponentType) }
                .toAnyArray(upperComponentType.upperClass)
        }
        val iterableSchema = toType.resolveIterableSchemaOrNull()
        if (iterableSchema === null) {
            return null
        }
        return if (iterable is Collection<*>)
            collectionMapTo(iterable, iterableSchema.rawClass, iterableSchema.componentType.deepUpperBound, converter)
        else
            iterableMapTo(iterable, iterableSchema.rawClass, iterableSchema.componentType.deepUpperBound, converter)
    }

    private fun iterableMapTo(
        iterable: Iterable<Any?>,
        iterableClass: Class<*>,
        componentType: Type,
        converter: Converter
    ): Iterable<Any?>? {
        return when (iterableClass) {
            List::class.java -> iterable.mapTo(LinkedList<Any?>()) {
                converter.convert(it, componentType)
            }.toList()
            LinkedList::class.java -> iterable.mapTo(LinkedList()) {
                converter.convert(it, componentType)
            }
            ArrayList::class.java -> iterable.mapTo(ArrayList()) {
                converter.convert(it, componentType)
            }
            Collection::class.java, Set::class.java -> iterable.mapTo(LinkedHashSet<Any?>()) {
                converter.convert(it, componentType)
            }.toSet()
            LinkedHashSet::class.java -> iterable.mapTo(LinkedHashSet()) {
                converter.convert(it, componentType)
            }
            HashSet::class.java -> iterable.mapTo(HashSet()) {
                converter.convert(it, componentType)
            }
            TreeSet::class.java -> iterable.mapTo(TreeSet()) {
                converter.convert(it, componentType)
            }
            else -> null
        }
    }

    private fun collectionMapTo(
        collection: Collection<Any?>,
        iterableClass: Class<*>,
        componentType: Type,
        converter: Converter
    ): Iterable<Any?>? {
        return when (iterableClass) {
            List::class.java -> collection.mapTo(ArrayList<Any?>(collection.size)) {
                converter.convert(it, componentType)
            }.toList()
            LinkedList::class.java -> collection.mapTo(LinkedList()) {
                converter.convert(it, componentType)
            }
            ArrayList::class.java -> collection.mapTo(ArrayList(collection.size)) {
                converter.convert(it, componentType)
            }
            Collection::class.java, Set::class.java -> collection.mapTo(LinkedHashSet<Any?>(collection.size)) {
                converter.convert(it, componentType)
            }.toSet()
            LinkedHashSet::class.java -> collection.mapTo(LinkedHashSet(collection.size)) {
                converter.convert(it, componentType)
            }
            HashSet::class.java -> collection.mapTo(HashSet(collection.size)) {
                converter.convert(it, componentType)
            }
            TreeSet::class.java -> collection.mapTo(TreeSet()) {
                converter.convert(it, componentType)
            }
            else -> null
        }
    }
}

open class BeanConvertHandler(
    private val beanResolver: BeanResolver = BeanResolver.DEFAULT
) : AbstractConvertHandler() {

    override fun doConvertNull(toType: Type, converter: Converter): Any? {
        return NULL_VALUE
    }

    override fun doConvertNotNull(from: Any, fromType: Type, toType: Type, converter: Converter): Any? {
        return when (toType) {
            is Class<*> -> return doConvert0(from, toType, toType.deepUpperBound, converter)
            is ParameterizedType -> return doConvert0(from, toType.rawClass, toType.deepUpperBound, converter)
            else -> null
        }
    }

    private fun doConvert0(from: Any, toRawClass: Class<*>, toType: Type, converter: Converter): Any? {
        return when (toRawClass) {
            Map::class.java -> beanResolver.copyProperties(
                from,
                HashMap<Any, Any?>(),
                from.javaClass,
                toType,
                converter
            ).toMap()
            MutableMap::class.java, LinkedHashMap::class.java -> beanResolver.copyProperties(
                from,
                LinkedHashMap(),
                from.javaClass,
                toType,
                converter
            )
            HashMap::class.java -> beanResolver.copyProperties(
                from,
                HashMap(),
                from.javaClass,
                toType,
                converter
            )
            TreeMap::class.java -> beanResolver.copyProperties(
                from,
                TreeMap(),
                from.javaClass,
                toType,
                converter
            )
            else -> {
                val toInstance = toRawClass.toInstance<Any>()
                return beanResolver.copyProperties(from, toInstance, from.javaClass, toType, converter)
            }
        }
    }

    companion object {

        @JvmField
        val DEFAULT: BeanConvertHandler = BeanConvertHandler()
    }
}