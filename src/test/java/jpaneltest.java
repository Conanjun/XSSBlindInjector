
import java.awt.*;
import javax.swing.*;

public class jpaneltest extends JFrame{
    public jpaneltest() {
        JPanel jp1 = new JPanel(new GridLayout(3, 2,20,10));//3行2列 水平间距20 垂直间距10
//第一行
        JLabel jl1 = new JLabel("文字:");
        jl1.setHorizontalAlignment(SwingConstants.RIGHT);
        JTextField jtf1 = new JTextField(10);
        jtf1.setText("文本框文字");
        jp1.add(jl1);jp1.add(jtf1);
//第二行
        JLabel jl2 = new JLabel("文字:");
        jl2.setHorizontalAlignment(SwingConstants.RIGHT);
        JTextField jtf2 = new JTextField(10);
        jtf2.setText("文本框文字");
        jp1.add(jl2);jp1.add(jtf2);
//第三行
        JLabel jl3 = new JLabel("文字:");
        jl3.setHorizontalAlignment(SwingConstants.RIGHT);
        JTextField jtf3 = new JTextField(10);
        jtf3.setText("文本框文字");
        jp1.add(jl3);jp1.add(jtf3);

        add(jp1);

        setLayout(new FlowLayout());//流式布局

        setTitle("Demo");
        setSize(321,169);//大小
        setLocationRelativeTo(null);//居中
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String[] args) {
        new jpaneltest();
    }
}