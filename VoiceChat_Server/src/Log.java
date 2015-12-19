/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * server's log. instances of Server add entries to this log
 * @author dosse
 */
public class Log {
    private static String log="";
    public static void add(String s){log+=s+"\n";}
    public static String get(){return log;}
}
