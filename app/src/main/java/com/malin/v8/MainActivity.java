package com.malin.v8;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

/**
 * https://www.heqiangfly.com/2017/08/07/open-source-j2v8-getting-started/
 * https://eclipsesource.com/blogs/tutorials/getting-started-with-j2v8/
 * https://eclipsesource.com/blogs/2015/06/06/registering-java-callbacks-with-j2v8/
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
        Log.e(TAG, " JS result name = " + hockeyTeam.getString("name"));

        // 因为 V8Object 是底层 Javascript 对象的引用，那么我们也可以对这个对象进行操作，
        // 比如现在为Javascript增加新的属性，比如 hockeyTeam.add("captain", person);
        // 在进行了这一步操作之后，新添加的属性 captain 可以在Javascript中立刻被访问到。以下代码可以验证这一点：
        hockeyTeam.add("captain", person);
        Log.e(TAG, " JS result  " + runtime.executeBooleanScript("person === hockeyTeam.captain"));


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

}
