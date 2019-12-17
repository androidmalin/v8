package com.malin.v8;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;

/**
 * base:
 * https://www.heqiangfly.com/2017/08/07/open-source-j2v8-getting-started/
 * https://eclipsesource.com/blogs/tutorials/getting-started-with-j2v8/
 * <p>
 * JsCallJavaViaInterface
 * https://www.jianshu.com/p/f472c43c16db
 * https://eclipsesource.com/blogs/2015/06/06/registering-java-callbacks-with-j2v8/
 * https://eclipsesource.com/blogs/2016/07/27/java-methods-as-jsfunctions/
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BBBAAA";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View view) {
        helloV8();
        getJsObject();
        array();
        exeJsFunction();
        exeJsFunctionBase();
        exeJsFunctionBaseCall();
        JsCallJavaViaInterface();
        jsCallJavaViaReflection();
        javaCallJsFunctionViaFunction();
    }

    /**
     * 这段脚本将两个字符串连接起来并且返回了结果字符串的长度
     * <p>
     * 要使用J2V8，首先你必须创建一个运行时环境，J2V8为此提供了一个静态工厂方法。
     * 在创建一个运行时环境时，同时也会加载J2V8的本地库。
     * <p>
     * 一旦创建了运行时，就可以在其上执行脚本
     * 为了执行脚本，它提供了多个基于不同返回值的执行方法。
     * 在这个例子里，我们使用了 executeIntegerScript() 这个方法，
     * 因为脚本执行的结果是一个int类型的整数，并且不需要任何的类型转换和包装。
     * 当应用结束时，必须释放运行时。
     */
    private void helloV8() {
        V8 runtime = V8.createV8Runtime();
        int result = runtime.executeIntegerScript(""
                + "var hello = 'hello, ';\n"
                + "var world = 'world!';\n"
                + "hello.concat(world).length;\n");
        Log.e(TAG, "JS result = " + result);
        runtime.release();
    }

    /**
     * 获取Javascript对象
     * 使用J2V8可以从Java中获取javascript对象的句柄
     * <p>
     * V8Object还提供了一些其他有用的方法。
     * getKeys（）将返回与Object关联的键。
     * getType（String key）将返回与键关联的对象的类型。
     * 使用这两种方法，可以动态遍历复杂的对象图。
     */
    private void getJsObject() {
        V8 runtime = V8.createV8Runtime();
        runtime.executeVoidScript(""
                + "var person = {};\n"
                + "var hockeyTeam = {name : 'WolfPack'};\n"
                + "person.first = 'Ian';\n"
                + "person['last'] = 'Bull';\n"
                + "person.hockeyTeam = hockeyTeam;\n");
        //执行脚本后，可以通过其名称访问任何全局变量。

        V8Object person = runtime.getObject("person");
        V8Object hockeyTeam = person.getObject("hockeyTeam");
        Log.e(TAG, "JS result name = " + hockeyTeam.getString("name"));

        // 因为 V8Object 是底层 Javascript 对象的引用，那么我们也可以对这个对象进行操作，
        // 比如现在为Javascript增加新的属性，比如 hockeyTeam.add("captain", person);
        // 在进行了这一步操作之后，新添加的属性 captain 可以在Javascript中立刻被访问到。以下代码可以验证这一点：
        hockeyTeam.add("captain", person);
        Log.e(TAG, "JS result  " + runtime.executeBooleanScript("person === hockeyTeam.captain"));


        // 最后，当我们不再需要它们时，必须释放我们访问的V8Object。
        // 如果可以从根目录访问它们，它们仍将存在于JavaScript中。
        // 之所以需要释放它们，是因为它们是通过方法调用返回给我们的（请参见上面的第2点）。
        person.release();
        hockeyTeam.release();
        runtime.release();
    }


    private void array() {
        V8 runtime = V8.createV8Runtime();
        runtime.executeVoidScript(""
                + "var hockeyTeam = {name : 'WolfPack'};\n");
        V8Object hockeyTeam = runtime.getObject("hockeyTeam");
        V8Object player1 = new V8Object(runtime).add("name", "John");
        V8Object player2 = new V8Object(runtime).add("name", "Chris");
        V8Array players = new V8Array(runtime).push(player1).push(player2);
        hockeyTeam.add("players", players);
        player1.release();
        player2.release();
        players.release();
        for (String key : hockeyTeam.getKeys()) {
            Object value = hockeyTeam.get(key);
            Log.e(TAG, "key:" + key + ",value:" + value);
        }
    }

    /**
     * 除了执行脚本外，Java还可使用J2V8调用JavaScript函数。
     * 函数可以是全局函数，也可以附加到另一个Object，并且可以选择返回结果。
     */
    private void exeJsFunction() {
        V8 runtime = V8.createV8Runtime();
        runtime.executeVoidScript(""
                + "var hockeyTeam = {\n"
                + "name      : 'WolfPack',\n"
                + "players   : [],\n"
                + "addPlayer : function(player) {\n"
                + "              this.players.push(player);\n"
                + "              return this.players.length;\n"
                + "}\n"
                + "}\n");

        // 要从Java调用此方法，我们只需要hockeyTeam对象的句柄。
        // 使用句柄，我们可以像调用脚本一样调用函数。
        // 但是，与脚本不同，函数也可以传递V8Array参数。
        V8Object hockeyTeam = runtime.getObject("hockeyTeam");
        V8Object player1 = new V8Object(runtime).add("name", "John");
        V8Array parameters = new V8Array(runtime).push(player1);

        // 参数数组的元素映射到JavaScript函数的参数。
        // 数组中的参数数量与函数上指定的参数数量不需要匹配。
        // undefined将用作默认值。 最后，必须释放参数数组。
        int size = hockeyTeam.executeIntegerFunction("addPlayer", parameters);
        Log.e(TAG, "JS result size = " + size);
        parameters.release();
        player1.release();
        hockeyTeam.release();
        runtime.release();
    }


    /**
     * Java call js function
     */
    private void exeJsFunctionBase() {
        V8 runtime = V8.createV8Runtime();
        runtime.executeVoidScript(""
                + "function add(a, b){\n"
                + "    return a + b\n"
                + "}");
        V8Array v8Array = new V8Array(runtime).push(10).push(20);
        int result = runtime.executeIntegerFunction("add", v8Array);
        Log.e(TAG, "result:" + result);
        v8Array.release();
        runtime.release();
    }

    /**
     * Java call js function
     * <p>
     * 在JS中万物皆对象，函数也不例外。在 j2v8 中，一切 js 对象都用 V8Object 表示，
     * 我们可以直接将其强制转换为 V8Function。
     * <p>
     * V8Function 表示的就是一个 js 函数对象，它拥有 call() 方法可以直接被调用。
     */
    private void exeJsFunctionBaseCall() {
        V8 runtime = V8.createV8Runtime();
        runtime.executeVoidScript(""
                + "function add(a, b){\n"
                + "    return a + b\n"
                + "}");

        // 先判断 add 是不是一个函数
        if (runtime.getType("add") == V8.V8_FUNCTION) {
            V8Array args = new V8Array(runtime).push(100).push(200);
            Object addObject = runtime.getObject("add");
            if (addObject instanceof V8Function) {
                V8Function addFunction = (V8Function) addObject;
                int addResult = (int) addFunction.call(null, args);
                Log.e(TAG, "addResult:" + addResult);
                args.close();
                addFunction.close();
            }
        }
        runtime.release();
    }

    /**
     * Js 调用 Java function（注册 Java 回调）
     * 1.接口方式
     * <p>
     * java 函数必须先注册到 js 才可以被调用
     */
    private void JsCallJavaViaInterface() {
        V8 runtime = V8.createV8Runtime();

        // 要注册函数到 js，首先要创建一个类，并实现 JavaCallback 接口（如果 java 函数没有返回值，则实现 JavaVoidCallback 接口即可）。
        // 这两个接口均有一个 invoke(V8Object receiver, V8Array parameters) 函数，
        // 当被 js 调用时就会触发，这样就可执行 java 代码了
        JavaVoidCallback javaVoidCallback = new JavaVoidCallback() {
            // receiver 是此函数被调用时所基于的对象
            // parameters 是传入的参数列表，表现为一个 V8数组，可以从中提取 js 传入的各个参数。
            // 在这个例子中，print 不再基于 Global，而是基于 array[i] 被调用的，
            // 因此 array[i] 将被传入 java 作为 receiver. 最终 java 将依次输出 AA says Hi., BB says Hi., CC says Hi..
            @Override
            public void invoke(V8Object receiver, V8Array parameters) {
                Log.e(TAG, receiver.getString("first") + parameters.get(0) + " " + parameters.get(1));
                if (parameters.length() > 0) {
                    // 必须释放从参数列表中检索到的所有V8Object，因为它们是由于方法调用而返回给您的。
                    Object arg1 = parameters.get(0);
                    if (arg1 instanceof Releasable) {
                        ((Releasable) arg1).release();
                    }
                }
            }
        };

        // 注册到 js 全局函数，函数名为 `print`
        runtime.registerJavaMethod(javaVoidCallback, "print");

        // 先注册,后使用
        runtime.executeVoidScript("" +
                "var array = [{first:'AA'}, {first:'BB'}, {first:'CC'}];\n" +
                "for ( var i = 0; i < array.length; i++ ) {\n" +
                "  print.call(array[i], \" says Hi.\",\"Second\");" +
                "}");
        runtime.release();
    }


    /**
     * Js 调用 Java function
     * 2.反射
     * <p>
     * java 函数必须先注册到 js 才可以被调用
     * <p>
     * 在本例中，通过反射注册了现有 java 对象的方法。必须指定Java对象、方法的名称和参数列表。
     * 并且此处不是直接注册为全局函数，而是先创建了一个名为 console 的 js 对象，把函数注册到了此对象上，
     * <p>
     * 不难发现，通过接口方式注册，参数可以是动态的
     * 而通过反射注册，参数必须明确指定并且与 java 参数严格匹配，若参数不匹配则会异常。
     */
    public void jsCallJavaViaReflection() {
        V8 runtime = V8.createV8Runtime();
        Console console = new Console();
        V8Object v8Console = new V8Object(runtime);
        runtime.add("console", v8Console);

        v8Console.registerJavaMethod(console, "log", "jlog", new Class<?>[]{String.class});
        v8Console.registerJavaMethod(console, "error", "jerr", new Class<?>[]{String.class});
        v8Console.release();

        // 然后可以直接在 js 中调用 `console.jlog('hello, world')` 与 `console.jerr('hello, world')` 了。
        runtime.executeScript("console.jlog('hello, world');");
        runtime.executeScript("console.jerr('hello, world');");
        runtime.release();
    }

    @SuppressWarnings("unused")
    private static class Console {
        public void log(final String message) {
            Log.e(TAG, "[INFO] " + message);
        }

        public void error(final String message) {
            Log.e(TAG, "[ERROR] " + message);
        }
    }

    private void javaCallJsFunctionViaFunction() {
        V8 v8 = V8.createV8Runtime();
//        V8Function callback = V8Function(v8,
//                { receiver: V8Object, parameters: V8Array -> System.out.println(parameters.getInteger(0))
//                }
//                )
        V8Function callback = new V8Function(v8, new JavaCallback() {
            @Override
            public Object invoke(V8Object receiver, V8Array parameters) {
                System.out.println(parameters.getInteger(0));
                return null;
            }
        });
//        val arg = V8Array(v8).push(1).push(2).push(callback)
//        v8.executeVoidFunction("add", arg)
        V8Array arg = new V8Array(v8).push(1).push(2).push(callback);
    }


}
