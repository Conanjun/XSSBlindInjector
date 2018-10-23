import javax.swing.*;
import java.awt.*;

public class gridlayouttest extends JFrame{
    private static final long serialVersionUID = 1L;

    public gridlayouttest(){
        this.setTitle("test");
        Container con = this.getContentPane();
        this.setLayout(new GridLayout(7,3,5,5));        //设置7行3列垂直水平间隔为5
        for (int i=0;i<20;i++){
            con.add(new JButton("Button"+i));            //20个按钮
        }
        this.setVisible(true);
        this.setBounds(50, 50, 300, 500);
    }
    public static void main(String[] args) {
        new gridlayouttest();
    }
}