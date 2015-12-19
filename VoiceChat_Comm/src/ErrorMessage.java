
import javax.swing.JOptionPane;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author dosse
 */
public class ErrorMessage {
    public static void main(String args[]){
        JOptionPane.showMessageDialog(new JOptionPane(),"This is a library, not a program.\nPlease run VoiceChat_Server or VoiceChat_Client");
    }
}
