1. 替换base成对应的value,是否可以做 FieldReference
    1. 用getDefBoxes()
       -> 没法用, 自行手动记录

type Pointer = VarPointer(method, local, clazz) | FieldPointer(allocation, fieldName)
type Allocation(line, clazz)
type VarField(receiver: VarPointer, fieldName: String)
type CallSite(receiver: VarPointer?, abstract: Method, args: Seq[Pointer], returns: Option[Pointer], lineNumber: Int)

reachableMethod:    Set[Method]
worklist:           Queue[Pointer -> Set[Allocation]]
pointerGraph:       Graph[Pointer, DiEdge]
callGraph:          Set[CallSite, Method]
env:                Map[Pointer, Set[Allocation]]



```text
    public static void main(java.lang.String[])
    {
        playground.samples.Prototype $stack9, a, c, d, _state, _a, _b;
        playground.samples.Inherited $stack10, b;
        playground.samples.Stage $stack13, stage;
        java.lang.String[] args;
        args := @parameter0: java.lang.String[];
        $stack9 = new playground.samples.Prototype;
        specialinvoke $stack9.<playground.samples.Prototype: void <init>()>();
        a = $stack9;
        $stack10 = new playground.samples.Inherited;
        specialinvoke $stack10.<playground.samples.Inherited: void <init>()>();
        b = $stack10;
        c = virtualinvoke b.<playground.samples.Prototype: playground.samples.Prototype foo(playground.samples.Prototype)>(a);
        d = virtualinvoke a.<playground.samples.Prototype: playground.samples.Prototype foo(playground.samples.Prototype)>(b);
        $stack13 = new playground.samples.Stage;
        specialinvoke $stack13.<playground.samples.Stage: void <init>()>();
        stage = $stack13;
        _state = virtualinvoke stage.<playground.samples.Prototype: playground.samples.Prototype foo(playground.samples.Prototype)>(stage);
        _a = virtualinvoke stage.<playground.samples.Prototype: playground.samples.Prototype foo(playground.samples.Prototype)>(a);
        _b = virtualinvoke stage.<playground.samples.Prototype: playground.samples.Prototype foo(playground.samples.Prototype)>(b);
        return;
    }



    // State.foo
    playground.samples.Prototype foo(playground.samples.Prototype)
    {
        playground.samples.Stage this;
        int $stack2;
        playground.samples.Inherited $stack3;
        playground.samples.Prototype source;
        this := @this: playground.samples.Stage;
        source := @parameter0: playground.samples.Prototype;
        $stack2 = this.<playground.samples.Stage: int b>;
        if $stack2 <= 20 goto label1;
        return source;
     label1:
        $stack3 = new playground.samples.Inherited;
        specialinvoke $stack3.<playground.samples.Inherited: void <init>()>();
        return $stack3;
    }

```









