## Introduction

Cast Pipelines (CP) are used to convert, validate and analyze input arguments of FastR builtins. The aim is to make the code in builtin specializations cleaner by relieving them from the burden of repeated argument handling that often leads to duplicate boilerplate code. Besides that, the declarative nature of CP allows for static analysis of pipelines, which is used to diagnose builtins by a special tool (mx rbdiag).

CP provides an API through which a _pipeline_ can be constructed declaratively for each builtin argument. This pipeline consists of one or more steps representing specific operations with the given argument. Because of the declarative character of the argument processing pipelines it is possible to retrieve additional information from the pipelines, which may be further used in tests or code analysis. For instance, each pipeline provides a set of output types, which may be used for coverage analysis of the builtin specializations, i.e. to check if no specialization is missing or unused. Also, in many cases it is possible to determine specific argument values from pipelines, such as default values, limit values of value intervals, and allowed or forbidden values (a.k.a. corner-case argument values). These specific values may be collected as argument samples and used to create automated tests of builtins (a.k.a. chimney-sweeping). The argument samples are naturally divided into the positive and negative sample sets. The positive samples can be used to test the builtin's functionality and compare the result with the result of the builtin's GnuR counterpart. On the the other hand, the negative samples, which are assumed to cause an error in the FastR builtin, can be used to determine if the GnuR version fails too and produces the same error; if not, the pipeline must be redesigned to reflect the original.

The following sections deal with the description of the CP API and with some implementation details.

## Cast Pipelines API

### Basics: usage in builtins

CP API is available through the `NodeWithArgumentCasts.Casts` class. For every builtin there is one instance of that class instantiated in the static block of the builtin class. The static block is also the place where the cast pipelines are constructed for every argument of the builtin requiring some conversion or validation, as shown in the following listing:

```java
    // in MyBuiltin class
    static {
    	Casts casts = new Casts(MyBuiltin.class);
    	
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

In case the builtin does not declare any cast pipeline from any reason, it should declare this fact as follows:

```java
    // in MyBuiltin class
	static {
		Casts.noCasts(MyBuiltin.class);
	}
```

The CP API for the pipeline construction is designed in the fluent-builder style. A new pipeline for an argument is initialized by calling the `arg` method on the `Casts` instance. The `arg` method accepts either the index of the argument or the name of the argument, while the latter case is preffered. Following the `arg` method is a series of steps, in which each step inserts, behind the scenes, a special Truffle nodes (`CastNode`) into the cast pipeline. The flow of a pipeline can be divided into four phases: _pre-initial_, _initial_, _coerced_ and _head_, while the last three are optional and each phase may consist of zero or more steps.

In the **pre-initial** phase, in addition to the **initial** phase, one can configure the overall properties and behaviour of the pipeline (see `PipelineConfigBuilder`). The pipeline can be configured using the `conf(Consumer)` step (declared in `PreinitialPhaseBuilder`).

The **initial** and **pre-initial** phases handle the input argument as a generic object, which may be subjected to various assertions and conversions. The argument may, but may not, exit the initial phase as an instance of a specific type. The pipeline declared in the following listing specifies the default error of the pipeline and inserts one assertion node checking whether the argument is a non-null string. The argument exits this pipeline as a string represented by the `RAbstractStringVector` class. If any of the two conditions fails, the default error is raised.

```java
  casts.arg("m").defaultError(RError.Message.MUST_BE_STRING, "msg1").
  mustBe(nullValue().not().and(stringValue()));
```

The argument enters the **coerced** phase after having been processed by a node inserted by one of the `as_X_Vector` pipeline steps, where `X` is the type of the resulting vector. The input type of the argument corresponds to the used `as_X_Vector` step. The following example illustrates a pipeline, where the initial phase consists of one step (`asIntegerVector`) and the coerced phase has no step.

```java
casts.arg("m").asIntegerVector();
```

The next listing shows a pipeline having two steps belonging to the initial phase (i.e. `mustBe(stringValue()).asStringVector()`) and also two steps in the coerced phase (i.e. `mustBe(singleElement()).findFirst()`).

```java
  casts.arg("quote").
  mustBe(stringValue()).
  asStringVector().
  mustBe(singleElement()).
  findFirst();
```

The `singleElement()` condition requires that the string vector contain exactly one element. The `findFirst()` step retrieves the first element of the vector argument (the head) while exiting the coerced phase and entering the head phase.

In the **head** phase, the argument value corresponds to the first element of the vector processed in the preceding coerced phase and may be handled basically by the same steps as in the initial phase, except the `as_X_Vector` steps. The head phase in the following example consists of two steps - `mustBe(logicalNA().not())` and `map(toBoolean())` - where the former asserts that the head of the logical vector argument must not be `NA` and the latter converts the logical value from the vector's head to the corresponding `boolean` value. The `findFirst` step in the coerced phase picks the head of the vector or returns the `RRuntime.LOGICAL_FALSE` in case the vector is empty.

```java
  casts.arg("na.encode").
  asLogicalVector().
  findFirst(RRuntime.LOGICAL_FALSE).
  mustBe(logicalNA().not()).
  map(toBoolean());
```

### Standalone usage

One can use the same API to only create a cast node not necessarily associated with an argument and possibly outside of the Builtins world. Entry point of this API is the static method `CastNodeBuilder.newCastBuilder()` and the final invocation should be `createCastNode()`. Example:

```java
import com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;
...
CastNode myCastNode = newCastBuilder().asStringVector().findFirst("default").buildCastNode();
```

### Class `Predef`

The `CastBuilder.Predef` class defines the `cast pipelines` DSL. By importing statically its content, all language elements, such as filters and mappers, become available.

```java
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*
```

### Steps

The following subsections are dealing with the specific types of pipeline steps.

#### Pseudosteps

There are two steps that actually insert no node into the pipeline. The `defaultError(RError.Message message, Object... args)` step declares the default error of the pipeline and the `defaultWarning(RError.Message message, Object... args)` step declares the default warning of the pipeline. These default messages may be overridden further in the pipeline in the error or warning producing steps. All such steps have their overloaded variants for specifiying alternative messages.

#### Modal Steps

There are two modal steps - `mustBe` and `shouldBe` - that establish assertions on the argument value. If the assertion fails, the former raises an error, while the latter outputs a warning. If no message is specified, the default one is used. These steps may be used in all phases.

The assertion conditions are passed as the first argument of those steps and must match the context argument type, which is `java.lang.Object` initially and may be made more specific further in the pipeline by certain steps, among which is also the `mustBe` one. The conditions are objects implementing the `ArgumentFilter<T, R extends T>` interface. Under normal circumstances, the user is not supposed to implement custom conditions. Instead, there is a set of predefined conditions in the `CastBuilder.Predef` class, which should contain all conditions occuring in the original GnuR code, although some may be missing, while others may be added.

The `ArgumentFilter<T, R extends T>` contains the logical operators `and`, `or` and `not`, which allows constructing more complex assertions from the elementary building blocks, as shown in the following listing. The expression asserts that the argument value must be a non-null string or logical value.

```java
casts.arg("msg1").mustBe(nullValue().not().and(stringValue().or(logicalValue())));
```

The `mustBe` step changes the context argument type of the argument in the following steps accordingly to the used filter expression. For example, in the following pipeline the `mustBe` step asserts that the argument must be a vector. This assertion narrows the context argument type in the following steps to `RAbstractVector`. Therefore, the conditions requiring a vector as the input value, such as `size`, may be used in the subsequent `shouldBe` step.

```java
casts.arg("x").mustBe(abstractVectorValue()).shouldBe(size(2));
```

#### Mapping Steps

The mapping steps allow converting the argument from one type to another using the so-called mappers passed as the argument of those steps. There are three mapping steps: `map`, `mapIf` and `returnIf`. The first one always converts the argument using the mapper object passed as the first argument, while the second converts the argument only if the condition passed as the first argument is `true`. Both converted and non-converted arguments then proceed to the next step in the pipeline. The third one works as the second one, except that the converted value exits the pipeline without further processing.
Both `mapIf` and `returnIf` allow specifying the false mapping branch too. Interestingly, the `returnIf` can be specified with no mapping branch at all, which results in exiting the pipeline with the input argument if it matches the filter condition (it is equivalent to the true branch being the identity mapper).
The following pipeling returns NULL immediately without propagating it further in the pipeline:

```java
casts.arg("x").returnIf(nullValue()).asIntegerVector().findFirst();
```

The mapper object implements the `ArgumentMapper<S, R>` interface, nevertheless, just as in the case of the filter conditions, the user is not expected to implement custom ones, since the `CastBuilder.Predef` class should contain all necessary mappers.

The mapping steps may be used in all phases except the coerced phase, for the time being.

The signatures of the mapping steps are:

```java
map(Mapper<T, S> mapFn)
mapIf(Filter<? super T, ? extends S> argFilter, Mapper<S, R> trueBranchMapper)
mapIf(Filter<? super T, ? extends S> argFilter, Mapper<S, R> trueBranchMapper, Mapper<S, R> falseBranchMapper)
returnIf(Filter<? super T, ? extends S> argFilter)
returnIf(Filter<? super T, ? extends S> argFilter, Mapper<S, R> trueBranchMapper)
returnIf(Filter<? super T, ? extends S> argFilter, Mapper<S, R> trueBranchMapper, Mapper<S, R> falseBranchMapper)
```

A usage of the unconditional map step is illustrated in the following listing, where a logical value is mapped to a boolean value using the `toBoolean()` mapper. Since the unconditional mapping changes the context argument type according to the output type of the used mapper, it is possible to append the `shouldBe` modal step with the `trueValue()` filter requiring a boolean value on its input.

```java
  casts.arg("na.rm").
  asLogicalVector().
  findFirst().
  map(toBoolean()).
  shouldBe(trueValue());
```

In the following pipeline **only** null values are converted to the empty string in the initial phase. In contrast to the unconditional mapping, the conditional mapping reduces the context type to `Object`.

```java
casts.arg("quote").mapIf(nullValue(), constant(""));
```

#### Vector Coercion Steps

The vector coercion steps coerce the input argument value to the specified vector type. These steps can be used in the initial phase only. There are the following steps available:

*   `asIntegerVector()`: coerces the input value to `RAbstractIntVector` (see `CastIntegerNode`)
*   `asDoubleVector()`: coerces the input value to `RAbstractDoubleVector` (see `CastDoubleNode`)
*   `asLogicalVector()`: coerces the input value to `RAbstractLogicalVector` (see `CastLogicalNode`)
*   `asStringVector()`: coerces the input value to `RAbstractStringVector` (see `CastStringNode`)
*   `asComplexVector()`: coerces the input value to `RAbstractComplexVector` (see `CastComplexNode`)
*   `asRawVector()`: coerces the input value to `RAbstractRawVector` (see `CastRawNode`)
*   `asVector()`: coerces the input value to `RAbstractVector` (see `CastToVectorNode`)

All these steps terminate the initial phase and start the coerced phase.

#### findFirst

The `findFirst` step retrieves the first element from the vector argument. The output type corresponds to the element type of the vector.

This step is available in the coerced phase only. It comes in a couple of flavours, which differ in how they handle a missing first element.

```java
findFirst()
findFirst(RError.Message message, Object... messageArgs)
<E>findFirst(E defaultValue)
<E>findFirst(E defaultValue, RError.Message message, Object... messageArgs)
findFirstOrNull()
```

The first variant raises the default error of the pipeline as long as the vector is empty, the second variant throws the error specified in its arguments, the third one returns the default value instead of raising an error and the fourth one returns the default value and prints the warning specified in its arguments. The fifth one returns NULL if the vector argument is empty.

#### notNA

The `notNA` step reacts on NA argument values. This step is available in the initial and head phases only and also comes in four flavours.

```java
notNA()
notNA(RError.Message message, Object... messageArgs)
notNA(T naReplacement)
notNA(T naReplacement, RError.Message message, Object... messageArgs)
```

Analogously to the findFirst step, the no-arg version throws the default error if the argument value is an NA, while the second version throws the specified error. The next two versions return the `naReplacement` value, instead of raising an exception, while the last version prints the specified warning.

The notNA step does not change the context argument type.

### Handling of RNull and RMissing values

By default, `RNull` and `RMissing` argument values __are sent to the pipeline__. While most of the pipeline cast nodes ignore those values and let them pass through, there are some nodes that may perform some transformation of those values. For example, the `FindFirstNode` node replaces both `RNull` and `RMissing` by the replacement values specified in the corresponding `findFirst(repl)` pipeline step. Also the `CastToVectorNode` coercion node replaces those values by an empty list provided that the `isPreserveNonVector` flag is set  (i.e. `asVector(true)`) .

The following list summarizes the behavior of a couple of pipeline steps with regard to the special values:

*   `as<TYPE>()` coercions: RNull/RMissing pass through, except `asVector(true)`, which converts null to the empty list
*   `findFirst`: RNull/RMissing treated as an empty vector, i.e. if the default value is specified it is used as a replacement for the special value, otherwise an error is raised.
*   `notNA`: RNull/RMissing pass through
*   `mustBe`, `shouldBe`: the filter condition determines whether RNull/RMissing is let through, e.g. `mustBe(stringValue())` blocks NULL since it is obviously not a string value. On the other hand, `shouldBe(stringValue())` lets the NULL through. On the other hand, `mustBe(nullValue().or(stringValue))` lets NULL pass.
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

NULL does not pass through the pipeline. MISSING passes through. All the following pipelines are equivalent:

```java
    cb.arg("x").mustBe(nullValue().not()).mustBe(stringValue().not());
    cb.arg("x").mustBe(nullValue().not().and(stringValue().not()));
    cb.arg("x").mustBe((nullValue().or(stringValue())).not());
    cb.arg("x").mustNotBeNull().mustBe(stringValue().not());
```

Neither NULL nor MISSING pass through the pipeline:

```java
    cb.arg("x").asStringVector().mustBe(singleElement());
```

### Cast Pipeline Optimizations

The declarative character of cast pipelines allows for a number of optimizations done during the pipeline construction (some of them are mentioned in the previous section).

#### FindFirst optimization

Provided that the pipeline contains the `findFirst(x)` step specifying the replacement `x`, than the `RNull/RMissing` values are routed directly before the `FindFirstNode`. If the pipeline contains the overloaded versions `findFirst()` or `findFirst(error)`, the pipeline is configured to throw the error associated with the `FindFirstNode` for `RNull/RMissing` argument values.

#### Scalar values optimization

String, double and integer argument values are routed directly after the `FindFirstNode`. On the other hand, logical values bypass the whole pipeline provided that the pipeline ends by the pattern `asLogicalVector()...findFirst().map(toBoolean())`.

For more details see `BypassNode`.

### Sample Builtins Using Cast Pipelines

* [Scan.java](../../com.oracle.truffle.r.nodes.builtin/src/com/oracle/truffle/r/nodes/builtin/base/Scan.java)
* [NGetText.java](../../com.oracle.truffle.r.nodes.builtin/src/com/oracle/truffle/r/nodes/builtin/base/NGetText.java)

### Using `rbdiag` tool

The `mx rbdiag` tool diagnoses a given builtin or all builtins. This command prints a brief report for each cast pipeline defined in a given builtin including a summary of the diagnosis.

#### Result type analysis

This analysis provides information about all possible result types of each argument and how those result types are bound to the corresponding arguments of the builtin's specializations. Importantly, the report can reveal unbound argument types that can potentially cause the missing specialization error. Further, it can reveal the so-called __dead__ specializations, i.e. specializations that are never invoked.

This analyis can be illustrated on the `rowsum_matrix` builtin. The `mx` command is then as follows:

```bash
mx rbdiag rowsum_matrix
```

It prints a rather lengthy report, of which the following snippet shows the diagnostics of the `x` argument cast pipeline:

```bash
 Pipeline for 'x' (arg[0]):  
  Warning: Unbound types:  
   [Integer, Double]  
  Result types union:  
   [Integer, RAbstractIntVector, RAbstractDoubleVector, Double]  
  Bound result types:  
   potential (RAbstractIntVector->RVector) in Object rowsum(*RVector*,RVector,RVector,boolean,RStringVector)  
   potential (RAbstractDoubleVector->RVector) in Object rowsum(*RVector*,RVector,RVector,boolean,RStringVector)  
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
```

The produced result types are naturally the same ones, but now all of them are bound. In conclusion, using the abstract types resulted in the binding of the previously unbound types.

Note: The `full` flag indicates that the pipeline's type is a subtype of the specialization's argument type. The `partial` flag, which is not seen here, indicates that the pipeline's type is a supertype of the specialization's argument type. The `potential` flag indicates an implicit conversion or a potentially non-empty intersection between the two types, typically if they are both interfaces.

#### Chimney-sweeping

In the dynamic stage, the tool uses the samples inferred from the cast pipelines of a given builtin along with some 'springboard' list of valid arguments to construct a number of argument combinations. These argument lists are then used to invoke both the FastR builtin and its GnuR counterpart. The tool uses the test output file (ExpectedTestOutput.test) to obtain the 'springboard' argument lists for a given builtin. These valid argument lists are reproduced by substituting combinations of generated samples. The varied arguments are then used against both GnuR and FastR. If the two outputs differ, the tool reports the command and the corresponding outputs.

The tool can be run from the command line via the mx command:

```bash
mx rbdiag colSums --sweep
```

To filter out stack traces, the error output can be redirected as follows:

```bash
mx rbdiag colSums --sweep 2> /dev/null
```

The following command sweeps all builtins using a less strict method for comparing FastR and GNUR outputs (`–matchLevel=error`), according to which two outputs are considered matching if both contain `Error` or none of them contains `Error`.

```bash
mx rbdiag --sweep --mnonly --matchLevel=error --maxSweeps=30 --outMaxLev=0
```

The command also ignores the samples yielded by the cast pipelines and uses `RNull` and `RMissing` as the only samples for all arguments (`–mnonly`). The `--maxSweeps=30` restricts the maximum number of sweeps per builtin to 30 and the `--outMaxLev=0` option sets the least detailed level of verbosity.
