import javax.swing.*;
import java.awt.*;

public class flowlayouttest extends JFrame {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public flowlayouttest() {
        new JFrame("title");
        Container con = getContentPane();
        setLayout(new FlowLayout(0, 10, 10));            //按照左对齐排列垂直水平间隔为10
        for (int i = 0; i < 10; i++) {
            add(new JButton("Button" + i));              //添加组件
        }
        this.setVisible(true);                         //设置窗体可见
        this.setSize(300, 300);                        //设置窗体大小
    }

    public static void main(String[] args) {
        flowlayouttest a = new flowlayouttest();
    }
}