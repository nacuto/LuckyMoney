package com.cjt.luckymoney;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Handler;
import android.os.Message;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * Created by CJT on 2018/5/19.
 */

public class MonitorService extends AccessibilityService {

    private static final int MSG_BACK = 233;
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_BACK) {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 获取事件的类型，由于在配置文件中接收的事件只有三种类型，所以只可能有三种情况：
        // 窗口状态变化，通知栏状态变化，窗口内容变化
        final int eventType = event.getEventType();
        // 通知栏状态变化事件
        if ( eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED ){
             // 查看通知栏的文本信息
            List<CharSequence> textList = event.getText();
            if ( !textList.isEmpty() ){
                // 遍历所有信息分别匹配“[微信红包]”字样
                for( CharSequence text : textList ){
                    if ( String.valueOf(text).contains("[微信红包]") ){
                        // 判断Data非空，且是Notification对象
                        if(event.getParcelableData()==null ||
                           !(event.getParcelableData() instanceof Notification))
                            return;
                        Notification notification = (Notification)event.getParcelableData();
                        // 使用PendingIntent延后并跳转到相应的微信聊天界面
                        PendingIntent pendingIntent = notification.contentIntent;
                        try {
                            pendingIntent.send();
                        }catch (PendingIntent.CanceledException e){}
                        break;
                    }
                }
            }
        }
        // 窗口状态变化事件或窗口内容变化事件
        // 通过思路，我们知道，这两类事件都是在微信界面触发，只有四种情况
        // 1. 微信列表界面 2. 微信聊天界面 3. 微信拆红包界面 4. 拆完红包后的界面
        else {
            // 如果是在拆红包界面
            if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI"
                 .equals(event.getClassName().toString())){
                // 获取当前界面的祖先节点
                AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                System.out.println(nodeInfo.getChildCount());
                if(nodeInfo==null) return;
                // 遍历它的所有子节点，如果有Button控件，证明是“开”按钮，则模拟点击
                for(int i=nodeInfo.getChildCount()-1 ; i>=0; i--){
                    AccessibilityNodeInfo node = nodeInfo.getChild(i);
                    if ( node!=null && node.getClassName().equals("android.widget.Button") ){
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        break;
                    }
                }
            }
            // 如果是拆完红包后的界面
            else if ( "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI"
                      .equals(event.getClassName().toString()) ){
                // 延时100ms后，调用handler线程执行全局后退的操作，使界面返回到聊天界面
                handler.sendEmptyMessageDelayed(MSG_BACK, 100);
            }
            // 如果是微信聊天界面或者微信列表界面
            else{
                // 获取当前界面的祖先节点
                AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                if(nodeInfo==null) return;
                // 假设是在微信聊天界面，匹配“领取红包”字样
                List<AccessibilityNodeInfo> ChatNodes =
                        nodeInfo.findAccessibilityNodeInfosByText("领取红包");
                if (null != ChatNodes && ChatNodes.size() != 0){
                    // 如果存在，则找到其父节点（能点击的控件LinearLayout）模拟点击
                    for(int i=ChatNodes.size()-1 ; i>=0; i--){
                        AccessibilityNodeInfo node = ChatNodes.get(i).getParent();
                        if ( node!=null && node.isClickable() ){
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                    }
                }
                // 假设是在微信列表界面，由于显示聊天信息用的控件为View，不能使用ByText进行匹配
                // 于是使用ByViewId进行匹配，得到8个列表内容
                List<AccessibilityNodeInfo> ListNodes =
                        nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/apx");
                if (null != ListNodes && ListNodes.size() != 0){
                    for(int i=ListNodes.size()-1 ; i>=0; i--){
                        // 对这8个列表内容的文本匹配“[微信红包]”字样
                        if ( !ListNodes.get(i).getText().toString().contains("[微信红包]") ) continue;
                        // 如果存在红包，则找到其父节点（能点击的控件LinearLayout）模拟点击，进入聊天界面
                        AccessibilityNodeInfo node = ListNodes.get(i).getParent();
                        if ( node!=null && node.isClickable() ){
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                    }
                }
            }
        }
    }
    @Override
    public void onInterrupt() {

    }
}
