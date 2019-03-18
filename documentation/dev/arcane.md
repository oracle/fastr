
### RCallNode AST schema

RCallNode represents and executes the syntax node for a function call.
	
#### default dispatch (RFunction)
	
	// Dispatches a call to a function for actual arguments
	FunctionDispatch
		// Reorders implicit actual arguments according to R's arg matching rules. See ArgumentMatcher
		PrepareArgumentsDefault
			// Used to increment temporarily the reference count of arguments until all arg nodes are evaluated
			ShareObjectNode
			// Used to decrement the reference count of arguments after all arg nodes have been evaluated
			UnShareObjectNode
		or
		// Reorders explicit actual arguments according to R's arg matching rules
		PrepareArgumentsExplicit

		// Evaluates arg promises and executes the builtin
		BuiltinCallNode(cachedTarget.builtinNode)
			// Used to force arg promises evaluation
			PromiseHelperNode[]
			// Used to force varargs promises evaluation
			PromiseCheckHelperNode
			// Executes the builtin
			RBuiltinNode
		or
		// Executes an R (non-builtin) function
		DispatchedCallNode
			// Executes a given function via the same call target (held by the DirectCallNode child node)
			CallRFunctionNode
				// Represents a direct call to a call target
				DirectCallNode
			// Used to execute the fast-path of the given function, if exists
			RFastPathNode(opt)

	Note: For S3 dispatch of non-builtins see S3DispatchFunctions such as UseMethod and NextMethod
	      For S4 dispatch look at StandardGeneric
	
	
#### internal generic dispatch of a builtin with implicit args (RFunction)
	
	// The dispatch arg node to be evaluated for the actual dispatch arg
	RNode
	// Stores the dispatch arg in a temporary slot
	TemporarySlotNode
	// Retrieves the basic function for the builtin
	GetBasicFunction(opt, used only if the dispatch arg is S4)
		// Retrieves the methods namespace 
		GetFromEnvironment
		// Retrieves .BasicFunsList from the methods namespace
		GetFromEnvironment
		// Retrieves the given basic function from .BasicFunsList
		AccessListField
	// dtto, for S4, the basic function is used instead 
	FunctionDispatch
	// Returns a string vector with the class hierarchy of the dispatch arg (S3 only)
	ClassHierarchyNode
	// Searches for the correct S3 method for the given function name and the vector of class names  (S3 only)
	S3FunctionLookupNode
	
	
#### internal generic dispatch of a builtin with explicit args (RFunction)
	
	// Reads the dispatch arg from the frame
	LocalReadVariableNode

	Note: the basic function for an S4 arg is not retrieved using GetBasicFunction as above (should be?)

	// dtto
	FunctionDispatch
	// dtto
	ClassHierarchyNode
	// dtto
	S3FunctionLookupNode
	
	
#### group generic dispatch of a builtin with explicit or implicit args (RFunction)

	Note: it comprises these builtin groups: MATH_GROUP_GENERIC, OPS_GROUP_GENERIC, SUMMARY_GROUP_GENERIC, COMPLEX_GROUP_GENERIC

	// Reads the args from the frame
	LocalReadVariableNode (opt, for explicit args only)
	// Encapsulates arg nodes, signature, var arg indices and utility methods
	CallArgumentsNode (opt, for implicit args only)
	// dtto, for the first arg
	ClassHierarchyNode
	// dtto, for the second arg, if exists
	ClassHierarchyNode
	// dtto, for the first arg
	S3FunctionLookupNode
	// dtto, for the second arg, if exists
	S3FunctionLookupNode
	// Used at various places to evaluate arg and other promises
	PromiseCheckHelperNode
	// dtto
	FunctionDispatch


#### foreign call (DeferredFunctionValue)

	// Invokes a possible foreign member
	ForeignInvoke
		// dtto
		CallArgumentsNode
		// Converts 'primitive' values from the outside world to internal FastR representations
		Foreign2R
		// Calls a foreign function using message INVOKE
		SendForeignInvokeMessage
			// Normalizes the internal FastR data representation for the outside world
			R2Foreign
			// The INVOKE message node
			Node

### Function Fast-Paths Infrastructure

The RRootNode class, which is the base class for FunctionDefinitionNode and RBuiltinRootNode, provides 
an optional facility for an optimised execution of its AST. The optimisation is centered
around the FastPathFactory interface, whose instance may be retrieved from an RRootNode using
the getFastPath method returning an instance of FastPathFactory or null, if the root node
does not support the fast path.

In terms of execution, the FastPathFactory#create() method is called by DispatchedCallNode
to obtain an instance of RFastPathNode. This node is then used in DispatchedCallNode#execute
to execute the fast path first. If the fast path execution fails, which is indicated by returning null,
the execution defaults to the original path.

Currently, the optimisation using FastPathFactory takes place in two classes: ArgumentMatcher
and CallMatcherCachedNode.

#### ArgumentMatcher

ArgumentMatcher, which is responsible for matching actual call argument nodes with the formal
arguments of a function, is able to perform two optimisations based on the information provided by FastPathFactory:
argument inlining and forced promise evaluation. The gist of both optimisations is that
the argument node is evaluated on the caller site. Otherwise, the AST of the called function would
aggregate AST trees of promises from various callers, which would lead to the swelling of the function's code 
(see PromiseHelperNode#generateValueDefault and InlineCacheNode). Furthermore, the argument promise could also be evaluated 
at more places in the function. Ideally, reading an argument in a function body
should be as simple as reading a value from the frame, which would be the case if the argument is evaluated
on the caller site. On the contrary, reading a value from a non-optimised promise leads to iterating an inline cache (InlineCacheNode)
caching promise ASTs from various callers to find the AST of the current caller and evaluating it.

##### Argument inlining
As long as the FastPathFactory declares that a given argument is always evaluated by the function,
then it safe to conclude that it is safe to evaluate the argument eagerly. In uch a case,
the argument node will be evaluated on the caller site and no argument promise will be created, i.e.
no promise node will be created either.

By default, builtins declare to evaluate all arguments. If some argument of a builtin is not evaluated, it is declared 
by adding its index to the nonEvalArgs attribute of the RBuiltin annotation.
On the other hand, non-builtin functions do not evaluate the arguments (see EvaluatedArgumentsFastPath) by default.
The default behavior can be overridden using RRootNode.setFastPath, as is done in BasePackage.loadOverrides,
where fast paths of several functions are overridden.

##### Forced promise evaluation

Unless an argument is evaluated, its argument node is wrapped up into a promise wrapper node,
which will be evaluated to obtain a promise for the argument. This promise is then passed to
the function call. Also in this case it is possible, under certain circumstances, to create
an optimised promise (OptForcedEagerPromiseNode), which eagerly evaluates the argument value
on the caller site. This scenario is carried out when FastPathFactory#forcedEagerPromise(argIndex)
returns true for a given argument index. See PromiseNode.create.

As to builtins, by default, the forcedEagerPromise method return false for all arguments. There are
exceptions to this rule, such as builtins cbind and rbind, which override the default behavior
by setting the FastPathFactory.FORCED_EAGER_ARGS fast path factory in BasePackage#loadOverrides.

As to non-functions, a special analysis of the body of a function is carried out in the constructor of
FunctionDefinitionNode to determine which argument can be evaluated on the caller site. To be qualified for that, an argument must be
read in any execution path of the function body and be "simple" (see EvaluatedArgumentsVisitor.isSimpleArgument).
For more details go to EvaluatedArgumentsVisitor.

#### CallMatcherCachedNode

TODO


### Incrementing/Decrementing Reference Count in Arguments

Before the value returned by an argument node is passed to a function call it may be subject to
incrementing its reference count. It is done by WrapArgumentNode after the argument value is evaluated
by the wrapped original argument node. The WrapArgumentNode uses the ArgumentStatePush node
to increment the ref count. ArgumentStatePush also registers a request for the later decrementing of the ref count,
which is done in the finally block of the function's execute method (i.e. FunctionDefinitionNode.execute).
The decrementing procedure and the register of the arguments, whose refcounts are to be decremented,
are encapsulated in PostProcessArgumentsNode, a child node of FunctionDefinitionNode. PostProcessArgumentsNode is created
by RASTBuilder and passed to the FunctionDefinitionNode constructor.

	// created for each "wrappable" argument node when creating CallArgumentsNode
	WrapArgumentNode
		// the original arg node
		RNode
		// increments the ref count of a shareable argument and registers the request for decrementing
		ArgumentStatePush



### `CALLER_FRAME` argument life-cycle 
(see `CallerFrameClosureProvider`)

	Legend:
	(*)     - a caller frame closure holding a materialised frame; it never occurs in the interpreter
	( )     - a caller frame closure with no frame; it never occurs in the compiler
	*       - a materialised closure
	^       - a request for the frame, e.g. stack introspection
	<opt>   - optimisation; the boundary between interpreted and compiled code
	<deopt> - deoptimisation; the boundary between compiled and interpreted code
 
#### The caller frame is available on the first call

	a) no stack introspection

	time
	------------------------------------->

	(*) (*) (*) <opt> ( ) ( ) ( )


	b) early stack introspection (i.e. in the interpreter)

	(*) (*)  *  <opt>  *   *   *
	     ^					

	c) late stack introspection (i.e. in the compiler)

	(*) (*) (*) <opt> ( )  ( ) <deopt>  *  *
	                        ^

  
#### The caller frame is not available on the first call

	time
	------------------------------------->

	a) no stack introspection

	( ) ( ) ( ) <opt> ( ) ( ) ( )


	b) early stack introspection (i.e. in the interpreter)

	( ) ( )  *  <opt>  *   *   *
         ^					

	c) late stack introspection (i.e. in the compiler)

	( ) ( ) ( ) <opt> ( )  ( ) <deopt>  *  *
                            ^
