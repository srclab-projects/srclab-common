package xyz.srclab.common.lang

import xyz.srclab.annotations.Acceptable
import xyz.srclab.annotations.Accepted
import java.io.StringWriter
import java.io.Writer
import java.util.*

/**
 * Chars template, to process a chars template with parameter and arguments:
 * ```
 * Map<Object, Object> args = new HashMap<>();
 * args.put("name", "Dog");
 * args.put("name}", "DogX");
 * args.put(1, "Cat");
 * args.put(2, "Bird");
 * CharsTemplate template1 = CharsTemplate.resolve(
 * "This is a {name}, that is a {}", "{", "}");
 * Assert.assertEquals(template1.process(args), "This is a Dog, that is a Cat");
 * CharsTemplate template2 = CharsTemplate.resolve(
 * "This is a } {name}, that is a {}}", "{", "}");
 * Assert.assertEquals(template2.process(args), "This is a } Dog, that is a Cat}");
 * ```
 * Chars template supports escape:
 * ```
 * CharsTemplate template3 = CharsTemplate.resolve(
 * "This is a } \\{{name\\}} ({name}), that is a {}\\\\\\{\\", "{", "}", "\\");
 * logger.log(template3.process(args));
 * Assert.assertEquals(template3.process(args), "This is a } {DogX (Dog), that is a Bird\\{\\");
 * ```
 * Note:
 * * Escape works in front of any token;
 * * Parameter suffix token can be used before parameter prefix token --
 *   in this case, it will be seem as a common text;
 */
interface CharsTemplate {

    /**
     * Source chars template as [String].
     */
    @Suppress(INAPPLICABLE_JVM_NAME)
    @get:JvmName("template")
    val template: String

    @Suppress(INAPPLICABLE_JVM_NAME)
    @get:JvmName("nodes")
    val nodes: List<Node>

    @JvmDefault
    fun process(
        args: Map<out @Acceptable(
            Accepted(String::class),
            Accepted(Integer::class),
        ) Any, Any?>
    ): String {
        val writer = StringWriter()
        process(writer, args)
        return writer.toString()
    }

    /**
     * Processes this template with [args].
     */
    @JvmDefault
    fun process(
        dest: Writer,
        args: Map<out @Acceptable(
            Accepted(String::class),
            Accepted(Integer::class),
        ) Any, Any?>,
    ) {
        for (node in nodes) {
            val value = node.text
            if (node.isText) {
                dest.write(value)
                continue
            }
            if (value.isEmpty()) {
                dest.write(args[node.parameterIndex].toString())
            } else {
                if (args.containsKey(value)) {
                    dest.write(args[value].toString())
                } else {
                    dest.write(args[node.parameterIndex].toString())
                }
            }
        }
    }

    interface Node {

        @Suppress(INAPPLICABLE_JVM_NAME)
        @get:JvmName("type")
        val type: Type

        @Suppress(INAPPLICABLE_JVM_NAME)
        @get:JvmName("text")
        val text: String

        /**
         * Parameter index, or -1 if this is not a parameter.
         */
        @Suppress(INAPPLICABLE_JVM_NAME)
        @get:JvmName("parameterIndex")
        val parameterIndex: Int

        @Suppress(INAPPLICABLE_JVM_NAME)
        @get:JvmName("isText")
        val isText: Boolean
            get() = (type == Type.TEXT)

        @Suppress(INAPPLICABLE_JVM_NAME)
        @get:JvmName("isParameter")
        val isParameter: Boolean
            get() = (type == Type.PARAMETER)

        enum class Type {
            TEXT, PARAMETER
        }
    }

    companion object {

        private const val ELLIPSES_NUMBER = 10

        @JvmName("resolve")
        @JvmStatic
        fun CharSequence.resolveTemplate(parameterPrefix: String, parameterSuffix: String): CharsTemplate {
            return WithoutEscape(this, parameterPrefix, parameterSuffix)
        }

        @JvmName("resolve")
        @JvmStatic
        fun CharSequence.resolveTemplate(
            parameterPrefix: String,
            parameterSuffix: String,
            escape: String
        ): CharsTemplate {
            return WithEscape(this, parameterPrefix, parameterSuffix, escape)
        }

        private class WithoutEscape(
            template: CharSequence,
            parameterPrefix: String,
            parameterSuffix: String
        ) : FromTokens() {

            override val template: String = template.toString()

            override val nodes: List<Node> by lazy {
                var prefixIndex = template.indexOf(parameterPrefix)
                if (prefixIndex < 0) {
                    return@lazy fromTokens(template, listOf(Token(0, template.length, Token.Type.TEXT)))
                }
                var startIndex = 0
                val tokens = LinkedList<Token>()
                while (prefixIndex >= 0) {
                    val suffixIndex = template.indexOf(parameterSuffix, prefixIndex + parameterPrefix.length)
                    if (suffixIndex < 0) {
                        throw IllegalArgumentException(
                            "Cannot find suffix after prefix at index: $prefixIndex (${
                                template.subSequence(prefixIndex, template.length).ellipses(
                                    ELLIPSES_NUMBER
                                )
                            })"
                        )
                    }
                    tokens.add(Token(startIndex, prefixIndex, Token.Type.TEXT))
                    tokens.add(Token(prefixIndex, prefixIndex + parameterPrefix.length, Token.Type.PREFIX))
                    if (prefixIndex + parameterPrefix.length < suffixIndex) {
                        tokens.add(Token(prefixIndex + parameterPrefix.length, suffixIndex, Token.Type.TEXT))
                    }
                    tokens.add(Token(suffixIndex, suffixIndex + parameterSuffix.length, Token.Type.SUFFIX))
                    startIndex = suffixIndex + parameterSuffix.length
                    prefixIndex = template.indexOf(parameterPrefix, startIndex)
                }
                if (startIndex < template.length) {
                    tokens.add(Token(startIndex, template.length, Token.Type.TEXT))
                }
                fromTokens(template, tokens)
            }
        }

        private class WithEscape(
            template: CharSequence,
            parameterPrefix: String,
            parameterSuffix: String,
            escape: String
        ) : FromTokens() {

            override val template: String = template.toString()

            override val nodes: List<Node> by lazy {
                val tokens = LinkedList<Token>()
                var startIndex = 0
                var i = 0
                var inParameterScope = false
                while (i < template.length) {
                    if (template.startsWith(escape, i)) {
                        val nextIndex = i + escape.length
                        if (nextIndex >= template.length) {
                            break
                        }
                        tokens.add(Token(startIndex, i, Token.Type.TEXT))
                        startIndex = nextIndex
                        i = nextIndex + 1
                        continue
                    }
                    if (template.startsWith(parameterPrefix, i)) {
                        if (inParameterScope) {
                            throw IllegalArgumentException(
                                "Wrong token $parameterPrefix at index $i (${
                                    template.subSequence(i, template.length).ellipses(ELLIPSES_NUMBER)
                                })."
                            )
                        }
                        tokens.add(Token(startIndex, i, Token.Type.TEXT))
                        tokens.add(Token(i, i + parameterPrefix.length, Token.Type.PREFIX))
                        inParameterScope = true
                        i += parameterPrefix.length
                        startIndex = i
                        continue
                    }
                    if (template.startsWith(parameterSuffix, i) && inParameterScope) {
                        if (i > startIndex) {
                            tokens.add(Token(startIndex, i, Token.Type.TEXT))
                        }
                        tokens.add(Token(i, i + parameterSuffix.length, Token.Type.SUFFIX))
                        inParameterScope = false
                        i += parameterSuffix.length
                        startIndex = i
                        continue
                    }
                    i++
                }
                if (inParameterScope) {
                    throw IllegalArgumentException(
                        "Suffix not found since index $startIndex (${
                            template.subSequence(startIndex, template.length).ellipses(ELLIPSES_NUMBER)
                        })."
                    )
                }
                if (startIndex < template.length) {
                    tokens.add(Token(startIndex, template.length, Token.Type.TEXT))
                }
                fromTokens(template, tokens)
            }
        }

        private abstract class FromTokens : CharsTemplate {

            protected fun fromTokens(template: CharSequence, tokens: List<Token>): List<Node> {
                if (tokens.isEmpty()) {
                    return emptyList()
                }
                val nodes = LinkedList<Node>()
                var i = 0
                var start = i
                var parameterIndex = 0
                loop@ while (i < tokens.size) {
                    val token = tokens[i]
                    if (token.isText()) {
                        i++
                        while (i < tokens.size) {
                            if (tokens[i].isText()) {
                                i++
                            } else {
                                nodes.add(newTextNode(template, tokens.subList(start, i)))
                                start = i
                                continue@loop
                            }
                        }
                        break
                    }
                    if (token.isPrefix()) {
                        i++
                        start = i
                        while (i < tokens.size) {
                            if (tokens[i].isText()) {
                                i++
                            } else if (tokens[i].isSuffix()) {
                                nodes.add(newParameterNode(template, parameterIndex, tokens.subList(start, i)))
                                i++
                                start = i
                                parameterIndex++
                                continue@loop
                            } else {
                                throw IllegalArgumentException(
                                    "Only text or suffix token is permitted after a prefix token."
                                )
                            }
                        }
                        break
                    }
                    throw IllegalArgumentException("Suffix token must after a prefix token.")
                }
                if (start != i) {
                    nodes.add(newTextNode(template, tokens.subList(start, i)))
                }
                return nodes
            }

            private fun newTextNode(template: CharSequence, tokens: List<Token>): Node {
                return object : Node {
                    override val type: Node.Type = Node.Type.TEXT
                    override val text: String by lazy {
                        tokens.joinToString("") { template.subSequence(it.startIndex, it.endIndex) }
                    }
                    override val parameterIndex: Int = -1
                }
            }

            private fun newParameterNode(template: CharSequence, parameterIndex: Int, tokens: List<Token>): Node {
                return object : Node {
                    override val type: Node.Type = Node.Type.PARAMETER
                    override val text: String by lazy {
                        tokens.joinToString("") { template.subSequence(it.startIndex, it.endIndex) }
                    }
                    override val parameterIndex: Int = parameterIndex
                }
            }
        }

        private data class Token(
            val startIndex: Int,
            val endIndex: Int,
            val type: Type,
        ) {
            fun isText(): Boolean {
                return type == Type.TEXT
            }

            fun isPrefix(): Boolean {
                return type == Type.PREFIX
            }

            fun isSuffix(): Boolean {
                return type == Type.SUFFIX
            }

            enum class Type {
                TEXT, PREFIX, SUFFIX
            }
        }
    }
}