# Interoperability

GraalVM supports several other programming languages, including JavaScript, Ruby, Python, and LLVM.
GraalVM implementation of R also provides an API for programming language interoperability that lets you execute code from any other language that GraalVM supports. Note that you must start the R script with `--polyglot` to have access to other GraalVM languages.

GraalVM execution of R provides the following interoperability primitives:
 - `eval.polyglot('languageId', 'code')` evaluates code in some other language, the `languageId` can be, e.g., `js`.
 - `eval.polyglot(path = '/path/to/file.extension')` evaluates code loaded from a file. The language is recognized from the extension.
 - `export('polyglot-value-name', rObject)` exports an R object so that it can be imported by other languages.
 - `import('exported-polyglot-value-name')` imports a polyglot value exported by some other language.

Use the `?functionName` syntax to learn more. The following example demonstrates the interoperability features:
```
# get an array from Ruby
x <- eval.polyglot('ruby', '[1,2,3]')
print(x[[1]])
# [1] 1

# get a JavaScript object
x <- eval.polyglot(path='r_example.js')
print(x$a)
# [1] "value"

# use R vector in JavaScript
export('robj', c(1,2,3))
eval.polyglot('js', paste0(
    'rvalue = Polyglot.import("robj"); ',
    'console.log("JavaScript: " + rvalue.length);'))
# JavaScript: 3
# NULL -- the return value of eval.polyglot
```
(Uses [r_example.js](https://www.graalvm.org/docs/examples/r_example.js).)

R vectors are presented as arrays to other languages. This includes single element vectors, e.g., `42L` or `NA`.
However, single element vectors that do not contain `NA` can be typically used in places where the other
languages expect a scalar value. Array subscript or similar operation can be used in other languages to access
individual elements of an R vector. If the element of the vector is not `NA`, the actual value
is returned as a scalar value, e.g. `int`. If the element is `NA`, then a special object that looks like `null`
is returned. The following Ruby code demonstrates this.

```ruby
vec = Polyglot.eval("R", "c(NA, 42)")
p vec[0].nil?
# true
p vec[1]
# 42

vec = Polyglot.eval("R", "42")
p vec.to_s
# "[42]"
p vec[0]
# 42
```

<p id='foreign'>The foreign objects passed to R are implicitly treated as specific R types.
The following table gives some examples.</p>

| Example of foreign object (Java) | Viewed 'as if' on the R side |
| -------------------------------- | ---------------------------- |
| int[] {1,2,3}                    | c(1L,2L,3L)                  |
| int[][] { {1, 2, 3}, {1, 2, 3} } | matrix(c(1:3,1:3),nrow=3)    |
| int[][] { {1, 2, 3}, {1, 3} }    | not supported: raises error  |
| Object[] {1, 'a', '1'}           | list(1L, 'a', '1')           |
| 42                               | 42L                          |

In the following code example, we can simply just pass the Ruby array to the R built-in function `sum`,
which will work with the Ruby array as if it was integer vector.

```
sum(eval.polyglot('ruby', '[1,2,3]'))
```

Foreign objects can be also explicitly wrapped into adapters that make them look like the desired R type.
In such a case, no data copying occurs if possible. The code snippet below shows the most common use cases.

```
# gives list instead of an integer vector
as.list(eval.polyglot('ruby', '[1,2,3]'))

# assume the following Java code:
# public class ClassWithArrays {
#   public boolean[] b = {true, false, true};
#   public int[] i = {1, 2, 3};
# }

x <- new('ClassWithArrays'); # see Java interop below
as.list(x)

# gives: list(c(T,F,T), c(1L,2L,3L))
```

For more details, please refer to
[the executable specification](https://github.com/oracle/fastr/blob/master/com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/library/fastr/R/interop-array-conversion-test.R#L158)
of the implicit and explicit foreign objects conversions.

Note that R contexts started from other languages or Java (as opposed to via the `bin/R` script) will default to non-interactive mode, similar to `bin/Rscript`.
This has implications on console output (results are not echoed) and graphics (output defaults to a file instead of a window), and some packages may behave differently in non-interactive mode.  

Bellow is a list of available FastR interoperability builtin functions.
For more information see the FastR help pages or try the examples.
```
> help(java.type)
> ?java.type
> example(java.type)
```

* `java.type`
* `java.addToClasspath`
* `is.polyglot.value`
* `eval.polyglot`
* `export`
* `import`

See the [Polyglot Programming](https://www.graalvm.org/docs/reference-manual/polyglot-programming/)
reference for more information about interoperability with other programming
languages.
