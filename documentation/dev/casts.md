## Introduction

Cast Pipelines (CP) are used to convert, validate and analyse input arguments of FastR builtins. The aim is to make the code in builtin specializations cleaner by relieving them from the burden of repeated argument handling that often leads to duplicate boilerplate code. Ideally, all argument handling code should be concentrated in a single method in a builtin. However, in certain situations, e.g. when a validation code evaluates more than one argument, the validation code cannot be moved to that single method.

CP provides an API through which a _pipeline_ can be constructed declaratively for each builtin argument. This pipeline consists of one or more steps representing specific operations with the given argument. Because of the declarative character of the argument processing pipelines it is possible to retrieve additional information from the pipelines, which may be further used in tests or code analysis. For instance, each pipeline provides a set of output types, which may be used for coverage analysis of the builtin specializations, i.e. to check if no specialization is missing or unused. Also, in many cases it is possible to determine specific argument values from pipelines, such as default values, limit values of value intervals, and allowed or forbidden values (i.e. corner-case argument values). These specific values may be collected as argument samples and used to create automated tests of builtins. The argument samples are naturally divided into the positive and negative sample sets. The positive samples can be used to test the builtin's functionality and compare the result with the result of the builtin's GnuR counterpart. On the the other hand, the negative samples, which are assumed to cause an error in the FastR builtin, can be used to determine if the GnuR version fails too and produces the same error; if not, the pipeline must be redesigned to reflect the original.

The following sections deal with the description of the CP API and with some implementation details.

## Cast Pipelines API

### Basics: usage in builtins

CP API is an extension of the `CastBuilder` class, whose instance is passed as the argument of the `createCasts` method that can be overridden in a builtin class to define custom argument conversions for the builtin. The body of the `createCasts` method is the place where the cast pipelines are constructed for every argument of the builtin requiring some conversion or validation, as shown in the following listing.

```java
    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("X").mustBe(numericValue(), RError.Message.X_NUMERIC);

        casts.arg("m").asIntegerVector().
                        findFirst().
                        notNA();

        casts.arg("n").asIntegerVector().
                        findFirst().
                        notNA();

        casts.arg("na.rm").asLogicalVector().
                        findFirst().
                        map(toBoolean());
    }
```

The CP API part for the pipeline construction is designed in the fluent-builder style. A new pipeline for an argument is initialized by calling `arg` method on the `CastBuilder` instance. The `arg` method accepts either the index of the argument or the name of the argument, while the latter case is preffered. Following the `arg` method is a series of steps, each inserting a special Truffle node (`CastNode`) into the cast pipeline of the argument. The flow of a pipeline can be divided into three phases: __pre-initial_, initial_, _coerced_ and _head_, while the last three are optional and each phase may consist of zero or more steps.

In the pre-initial phase one can configure the overall behavior of the pipeline. Currently, only the default handling of `RNull` and `RMissing` values can be overridden (the default behavior is explained below). The pipeline can be configured using `PreinitialPhaseBuilder#conf(Consumer)` or any other method of the `PreinitialPhaseBuilder` class, e.g. `PreinitialPhaseBuilder#allowNull()`.

An argument enters the initial phase as a generic object, which may be subjected to various assertions and conversions. The argument may, but may not, exit the initial phase as an instance of a specific type. The pipeline declared in the following listing specifies the default error of the pipeline and inserts one assertion node checking whether the argument is a non-null string. The argument exits this pipeline as a string represented by the `RAbstractStringVector` class. If any of the two conditions fails, the default error is raised.

```java
  defaultError(RError.Message.MUST_BE_STRING, "msg1").
  mustBe(notNull().and(stringValue()));</pre>
```

The argument enters the coerced phase after having been processed by a node inserted by one of the `as_X_Vector` pipeline steps, where `X` is the type of the resulting vector. The input type of the argument corresponds to the used `as_X_Vector` step. The following example illustrates a pipeline, where the initial phase consists of one step (`asIntegerVector`) and the coerced phase has no step.

```java
casts.arg("m").asIntegerVector();
```

The next listing shows a pipeline having two steps belonging to the initial phase (i.e. `mustBe(stringValue()).asStringVector()`) and also two steps in the coerced phase (i.e. `mustBe(singleElement()).findFirst()`).

```java
  casts.arg("quote").
  mustBe(stringValue()).
  asStringVector().
  mustBe(singleElement()).
  findFirst();</pre>
```

The `singleElement()` condition requires that the string vector contain exactly one element. The `findFirst()` step retrieves the first element (the head) while exiting the coerced phase and entering the head phase.

In the head phase, the argument value corresponds to the first element of the vector processed in the preceding coerced phase and may be handled basically by the same steps as in the initial phase, except the `as_X_Vector` steps. The head phase in the following example consists of two steps - `mustBe(notLogicalNA())` and `map(toBoolean())` - where the former asserts that the head of the logical vector argument must not be `NA` and the latter converts the logical value from the vector's head to the corresponding `boolean` value. The `findFirst` step in the coerced phase picks the head of the vector or returns the `RRuntime.LOGICAL_FALSE` in case the vector is empty.

```java
  casts.arg("na.encode").
  asLogicalVector().
  findFirst(RRuntime.LOGICAL_FALSE).
  mustBe(notLogicalNA()).
  map(toBoolean());</pre>
```

### Standalone usage

One can use the same API to only create a cast node not necessarily associated with an argument and possibly outside of the Builtins world. Entry point of this API is static method `CastNodeBuilder#newCastBuilder()` and the final invocation should be `createCastNode()`. Example:

```java
import com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;
...
CastNode myCastNode = newCastBuilder().asStringVector().findFirst("default").buildCastNode();
```

### Steps

The following subsections are dealing with the specific types of pipeline steps.

#### Pseudosteps

There are two steps that actually insert no node into the pipeline. The `defaultError(RError.Message message, Object... args)` step declares the default error of the pipeline and the `defaultWarning(RError.Message message, Object... args)` step declares the default warning of the pipeline. These default messages may be overridden further in the pipeline in the error or warning producing steps. All such steps have their overloaded variants for specifiying alternative messages.

#### Modal Steps

There are two modal steps - `mustBe` and `shouldBe` - that establish assertions on the argument value. If the assertion fails, the former raises an error, while the latter outputs a warning. If no message is specified, the default one is used. These steps may be used in all phases.

The assertion conditions are passed as the first argument of those steps and must match the context argument type, which is `java.lang.Object` initially and may be made more specific further in the pipeline by certain steps, among which is also the `mustBe` one. The conditions are objects implementing `ArgumentFilter<T, R extends T>` interface. Under normal circumstances, the user is not supposed to implement custom conditions. Instead, there is a set of predefined conditions in the `CastBuilder.Predef` class, which should contain all conditions occuring in the original GnuR code, although some may be missing.

```java
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*
```

The `ArgumentFilter<T, R extends T>` contains the logical operators `and`, `or` and `not`, which allows constructing more complex assertions from the elementary building blocks, as shown in the following listing. The expression asserts that the argument value must be a non-null string or logical value.

```java
casts.arg("msg1").mustBe(notNull().and(stringValue().or(logicalValue())));
```

The `mustBe` step changes the context argument type of the argument in the following steps accordingly to the used filter expression. For example, in the following pipeline the `mustBe` step asserts that the argument must be a scalar string value. This assertion narrows the context argument type in the following steps to `java.lang.String`. Therefore, the conditions requiring a string as the input value, such as `stringNA`, may be used in the subsequent `shouldBe` step.

```java
casts.arg("quote").mustBe(scalarStringValue).shouldBe(stringNA.not());
```

#### Mapping Steps

The mapping steps allow converting the argument from one type to another using the so-called mappers passed as the argument of those steps. There are two mapping steps: `map` and `mapIf`. The former always converts the argument using the mapper object passed as the first argument, while the latter converts the argument only if the condition passed as the first argument is `true`. The mapper object implements the `ArgumentMapper<S, R>` interface, nevertheless, just as in the case of the filter conditions, the user is not expected to implement custom ones, since the `CastBuilder.Predef` class should contain all necessary mappers.

The mapping steps may be used in all phases except the coerced phase, for the time being.

The signatures of the mapping steps are:

```java
map(ArgumentMapper<T, S> mapFn)
mapIf(ArgumentFilter<? super T, ? extends S> argFilter, ArgumentMapper<S, R> mapFn)
```

A usage of the unconditional is illustrated in the following listing, where a logical value is mapped to a boolean value using the `toBoolean`() mapper. Since the unconditional mapping changes the context argument type according to the output type of the used mapper, it is possible to append the `shouldBe` modal step with the `trueValue()` filter requiring a boolean value on its input.

```java
  casts.arg("na.rm").
  asLogicalVector().
  findFirst().
  map(toBoolean).
  shouldBe(trueValue());</pre>
```

In the following pipeline **only** null values are converted to the empty string in the initial phase. In contrast to the unconditional mapping, the conditional mapping does not change the context type.

```java
casts.arg("quote").mapIf(nullValue(), constant(""));
```

#### Vector Coercion Steps

The vector coercion steps coerce the input argument value to the specified vector type. These steps can be used in the initial phase only. There are the following steps available:

*   `asIntegerVector()`: coerces the input value to `RAbstractIntVector`
*   `asDoubleVector()`: coerces the input value to `RAbstractDoubleVector`
*   `asLogicalVector()`: coerces the input value to `RAbstractLogicalVector`
*   `asStringVector()`: coerces the input value to `RAbstractStringVector`
*   `asVector()`: coerces the input value to `RAbstractVector`

All these steps terminate the initial phase and starts the coerced phase.

#### findFirst

The `findFirst` step retrieves the first element from the vector argument. The output type corresponds to the element type of the vector.

This step is available in the coerced phase only. It comes in a couple of flavours, which differ in how they handle the missing the first element.

```java
findFirst()
findFirst(RError.Message message, Object... messageArgs)
<E>findFirst(E defaultValue)
<E>findFirst(E defaultValue, RError.Message message, Object... messageArgs)</pre>
```

The first variant raises the default error of the pipeline as long as the vector is empty, the second variant throws the error specified in its arguments, the third one returns the default value instead of raising an error and the fourth one returns the default value and prints the warning specified in its arguments.

#### notNA

The `notNA` step reacts on NA argument values. This step is available in the initial and head phases only and also comes in four flavours.

```java
notNA()
notNA(RError.Message message, Object... messageArgs)
notNA(T naReplacement)
notNA(T naReplacement, RError.Message message, Object... messageArgs)</pre>
```

Analogously to the findFirst step, the no-arg version throws the default error if the argument value is NA, while the second version throws the specified error. The next two versions return the `naReplacement` value, instead of raising an exception, while the last version prints the specified warning.

The notNA step does not change the context argument type.

### Handling of RNull and RMissing values

By default, `RNull` and `RMissing` argument values are sent to the pipeline. While most of the pipeline cast nodes ignore those values and let them pass through, there are some nodes that may perform some transformation of those values. For example, the `FindFirstNode` node replaces both `RNull` and `RMissing` by the replacement values specified in the corresponding `findFirstStep(repl)` pipeline step. Also the `CastToVectorNode` coercion node replaces those values by an empty list provided that the `isPreserveNonVector` flag is set.

The following list summarizes the behavior of a couple of pipeline steps with regard to the special values:

*   `as<TYPE>()` coercions: RNull/RMissing passing through
*   `findFirst`: RNull/RMissing treated as an empty vector, i.e. if the default value is specified it is used as a replacement for the special value, otherwise an error is raised.
*   `notNA`: RNull/RMissing passing through
*   `mustBe`, `shouldBe`: the filter condition determines whether RNull/RMissing is let through. For example, step `mustBe(stringValue())` blocks NULL since it is obviously not a string value. On the other hand, `mustBe(stringValue())` lets the NULL through.
*   `mapIf`: the condition behaves accordingly to the used filter (in the 1st parameter). For example, step `mapIf(stringValue(), constant(0), constant(1))` maps NULL to 1.

The following cast pipeline examples aim to elucidate the behavior concerning the special values.

Neither NULL nor MISSING pass through the pipeline:

```java
    cb.arg("x").mustBe(stringValue());
```

Therefore, the `mustNotBeNull` step in the following pipeline is redundant:

```java
    cb.arg("x").mustNotBeNull().mustBe(stringValue());
```

Both NULL and MISSING pass through the pipeline:

```java
    cb.arg("x").mustBe(stringValue().not());
```

NULL does not pass through the pipeline. MISSING passes through.

```java
    cb.arg("x").mustNotBeNull().mustBe(stringValue().not());
```

The same as above:

```java
    cb.arg("x").mustBe(nullValue().not().and(stringValue().not()));
```

Neither NULL nor MISSING pass through the pipeline:

```java
    cb.arg("x").asStringVector().mustBe(singleElement());
```

#### Overriding the default behavior

A cast pipeline can be configured not to send `RNull` and/or `RMissing` to the cast nodes forming the cast pipeline. Then those values either bypass the pipeline, being eventually transformed to some constant, or an error is raised. One can use the following steps in the pre-initial phase to override the default behavior:

*   `allowNull` - RNull bypasses the pipeline
*   `mustNotBeNull(errorMsg)` - the error with errorMsg is raised when the input argument is `RNull`
*   `mapNull(mapper)` - `RNull` is transformed using the mapper. The `RNull` replacement bypasses the pipeline.

Analogous methods exist for `RMissing`.

#### Optimizing the default behavior

The above-mentioned overriding configurations may also be applied behind-the-scenes as optimization of the pipelines with certain structure. For instance, `allowNull()` may be activated if the pipeline contains no `RNull/RMissing` handling cast node, such as `FindFirstNode` or `CastToVectorNode`. The `mustNotBeNull` configuration may optimize pipelines containing cast nodes raising an error for `RNull/RMissing`, such as the nodes produced by steps `findFirstStep()`, `findFirstStep(error)` or `mustBe(singleElement())`. Or the `mapNull(x)` configuration may be applied provided that the pipeline's last step is `findFirst(x)`.

### Cast Pipeline Optimizations

The declarative character of cast pipelines allows for a number of optimizations done during the pipeline construction (some of them are mentioned in the previous section).

#### FindFirst optimization

Provided that the pipeline contains the `findFirst(x)` step specifying the replacement `x`, `RNull/RMissing` values are routed directly before the `FindFirstNode`. If the pipeline contains the overloaded versions `findFirst()` or `findFirst(error)`, the pipeline is configured to throw the error associated with the `FindFirstNode` for `RNull/RMissing` argument values.

#### Scalar values optimization

String, double and integer argument values are routed directly after the `FindFirstNode`. On the other hand, logical values bypass the whole pipeline provided that the pipeline ends by the pattern `asLogicalVector()...findFirst().mapToBoolean()`.

### Sample Builtins Using Cast Pipelines

* [Scan.java](../../com.oracle.truffle.r.nodes.builtin/src/com/oracle/truffle/r/nodes/builtin/base/Scan.java)
* [NGetText.java](../../com.oracle.truffle.r.nodes.builtin/src/com/oracle/truffle/r/nodes/builtin/base/NGetText.java)

### Using `rbdiag` tool

The `mx rbdiag` tool implements the functionality of the static and dynamic stages of the chimney sweeping. This command prints a brief report for each cast pipeline defined in a given builtin.

#### Static stage

In the case of the static diagnostics the tool provides information about all possible result types of each argument and how those result types are bound to the corresponding arguments of the builtin's specializations. Importantly, the report can reveal unbound argument types that can potentially cause the missing specialization error.

It can be illustrated on the `rowsum_matrix` builtin. The `mx` command is then as follows:

```bash
mx rbdiag rowsum_matrix
```

It prints a rather lengthy report, of which the following snippet shows the diagnostics of the `x` argument cast pipeline:

```bash
 Pipeline for 'x' (arg[0]):  
  Result types union:  
   [Integer, RAbstractIntVector, RAbstractDoubleVector, Double]  
  Bound result types:  
   potential (RAbstractIntVector->RVector) in Object rowsum(*RVector*,RVector,RVector,boolean,RStringVector)  
   potential (RAbstractDoubleVector->RVector) in Object rowsum(*RVector*,RVector,RVector,boolean,RStringVector)  
  Unbound types:  
   [Integer, Double]  
```

The `Result types union` section lists all result types possibly produced by the pipeline. The `Bound result types` section outlines all bound type; i.e. the argument types for which there is a corresponding specialization in the builtin. In this case, only two argument types - `RAbstractIntVector` and `RAbstractDoubleVector` - are bound to the `rowsum(RVector,RVector,RVector,boolean,RStringVector)` specialization. (The asterisks surrounding the first argument in the specialization indicate the argument to which the type in question is bound). The `potential` flag indicates here that there is an underlying implicit conversion between the type produced by the cast pipeline and the argument type in the specialization.

Nevertheless, there are two arguments left unbound - `Integer` and `Double`. This problem can be easily healed by using the abstract vector types instead of the concrete ones. The following report snippet shows the diagnostics of the same `x` argument:

```bash
 Pipeline for 'x' (arg[0]):  
  Result types union:  
   [Integer, RAbstractIntVector, RAbstractDoubleVector, Double]  
  Bound result types:  
   full (RAbstractIntVector->RAbstractVector) in Object rowsum(*RAbstractVector*,RAbstractVector,RAbstractVector,boolean,RAbstractStringVector)  
   potential (Double->RAbstractVector) in Object rowsum(*RAbstractVector*,RAbstractVector,RAbstractVector,boolean,RAbstractStringVector)  
   full (RAbstractDoubleVector->RAbstractVector) in Object rowsum(*RAbstractVector*,RAbstractVector,RAbstractVector,boolean,RAbstractStringVector)  
   potential (Integer->RAbstractVector) in Object rowsum(*RAbstractVector*,RAbstractVector,RAbstractVector,boolean,RAbstractStringVector)  
  Unbound types:  
   []
```

The produced result types are naturally the same ones, but now all of them are bound. In conclusion, using the abstract types resulted in the binding of the previously unbound types.

Note: The `full` flag indicates that the pipeline's type is a subtype of the specialization's argument type. The `partial` flag, which is not seen here, indicates that the pipeline's type is a supertype of the specialization's argument type. The `potential` flag indicates an implicit conversion or a potentially non-empty intersection between the two types, typically if they are both interfaces.

#### Dynamic stage

In the dynamic stage, the tool uses the samples inferred from the cast pipelines of a given builtin along with some 'springboard' list of valid arguments to construct a number of argument combinations. These argument lists are then used to invoke both the FastR builtin and its GnuR counterpart. The tool uses the test output file (ExpectedTestOutput.test) to obtain the 'springboard' argument lists for a given builtin. These valid argument lists are reproduced by substituting combinations of generated samples. The varied arguments are then used against both GnuR and FastR. If the two outputs differ, the tool reports the command and the corresponding outputs.

The tool can be run from the command line via the mx command:

```bash
mx rbdiag colSums --sweep
```

To filter out stack traces, the error output can be redirected as follows:

```bash
mx rbdiag colSums --sweep 2> /dev/null
```

The following command sweeps all builtins using a less strict method for comparing FastR and GNUR outputs (`–matchLevel=error`), according to which two outputs are considered matching if both contains `Error` or none of them contains `Error`.

```bash
mx rbdiag --sweep --mnonly --matchLevel=error --maxSweeps=30 --outMaxLev=0
```

The command also ignores the samples yielded by the cast pipelines and uses `RNull` and `RMissing` as the only samples for all arguments (`–mnonly`). The `--maxSweeps=30` restricts the maximum number of sweeps per builtin to 30 and the `--outMaxLev=0` option sets the least detailed level of verbosity.
