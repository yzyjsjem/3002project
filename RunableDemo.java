import java.util.*;
import java.io.*;
import java.net.*;


class SubRunnable implements Runnable{

    
    public void run() {
        Thread.currentThread().setName("yinzhengjie");
        for (int i = 0; i < 3; i++) {
            try {
                //先当前线程休息1000毫秒，也就是我们所说的一秒
                Thread.sleep(1000);
                System.out.printf("线程【%s】还有【%d】秒结束进程\n",Thread.currentThread().getName(),3-i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}



public class RunableDemo {
    public static void main(String[] args) {
        //实例化我们自定义实现Runnable接口的方法
        SubRunnable sr = new SubRunnable();
        //创建Thread类对象，构造方法中，传递Runnable接口实现类
        Thread t = new Thread(sr);
        //调用Thread类方法start()方法
        t.start();
        for (int i = 0; i < 3; i++) {
            try {
                //先当前线程休息1000毫秒，也就是我们所说的一秒
                Thread.sleep(1000);
                System.out.printf("线程【%s】还有【%d】秒结束进程\n",Thread.currentThread().getName(),3-i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}



/*
以上代码执行结果如下：（发现执行结果依然是随机性的）
线程【main】还有【3】秒结束进程
线程【yinzhengjie】还有【3】秒结束进程
线程【main】还有【2】秒结束进程
线程【yinzhengjie】还有【2】秒结束进程
线程【yinzhengjie】还有【1】秒结束进程
线程【main】还有【1】秒结束进程
*/