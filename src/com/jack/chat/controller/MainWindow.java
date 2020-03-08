package com.jack.chat.controller;

import com.jack.chat.common.FriendPaneHolder;
import com.jack.chat.common.Session;
import com.jack.chat.component.FriendMenu;
import com.jack.chat.component.FriendPane;
import com.jack.chat.component.MessageCarrier;
import com.jack.chat.pojo.User;
import com.jack.chat.service.FriendService;
import com.jack.chat.service.imp.FriendServiceImpl;
import com.jack.chat.thread.ReceiveMessageService;
import com.jack.chat.util.MessageHandle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Jinkang He
 * @version 1.0
 * @date 2020/3/1 22:08
 */

public class MainWindow implements Initializable {
    /**
     * 总窗体
     */
    public GridPane root;
    /**
     * 第一行
     */
    public RowConstraints row1;
    /**
     * 第二行
     */
    public RowConstraints row2;
    /**
     * 主要窗口
     */
    public SplitPane main;
    /**
     * 好友列表区域板
     */
    public TabPane friendListPane;
    /**
     * 聊天消息呈现区
     */
    public ScrollPane messageAreaScrollPane;
    /**
     * 右边区域
     */
    public AnchorPane right;
    /**
     * 聊天对象
     */
    public Label chatWith;
    /**
     * 最小化
     */
    public Label minimize;
    /**
     * 最大化
     */
    public Label maximize;
    /**
     * 关闭
     */
    public Label close;
    public VBox friendList;
    public ImageView userAvatar;
    public Session session;
    public User user;
    public DataInputStream dis;
    public DataOutputStream dos;
    public User currentChatWith;
    public Label userName;
    public TextArea messageEditArea;
    private FriendPaneHolder friendPaneHolder = FriendPaneHolder.getInstance();
    private Double offsetX;
    private Double offsetY;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //关闭
        close.setOnMouseClicked(event -> {
            Platform.exit();
        });
        // 最小化
        minimize.setOnMouseClicked(event -> {
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setIconified(true);
        });

        // 最大化
        maximize.setOnMouseClicked(event -> {
            Stage stage = (Stage) root.getScene().getWindow();
            // 最大化，取消最大化
            stage.setMaximized(stage.maximizedProperty().not().get());
        });
        root.setOnMousePressed(event -> {
            Window window = root.getScene().getWindow();
            //             鼠标在屏幕中的坐标，    窗体在屏幕中的坐标
            this.offsetX = event.getScreenX() - window.getX();
            this.offsetY = event.getScreenY() - window.getY();
        });
        root.setOnMouseDragged(event -> {
            Window window = root.getScene().getWindow();
            //   新的鼠标位置-旧的鼠标位置+旧的窗体位置
            // = 鼠标的偏移量+旧的窗体位置
            window.setX(event.getScreenX() - this.offsetX);
            window.setY(event.getScreenY() - this.offsetY);
        });

        friendListPane.prefHeightProperty().bind(main.heightProperty());
        messageAreaScrollPane.prefHeightProperty().bind(main.heightProperty().multiply(0.6));
        messageAreaScrollPane.prefWidthProperty().bind(right.widthProperty());

        session = Session.getInstance();
        user = session.getUser();
        userAvatar.setImage(new Image("img/下载.jpg"));
        userName.setText(user.getNickName());
        dis = session.getDis();
        dos = session.getDos();
        FriendService friendService = FriendServiceImpl.getInstance();
        List<User> friendsList = friendService.getFriendsList(session.getUser().getAccount());
        for (User user : friendsList) {
            FriendPane friendPane = null;
            try {
                friendPane = new FriendPane(user);
                FriendPane finalFriendPane = friendPane;
                FriendPane finalFriendPane1 = friendPane;
                friendPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        if (event.getClickCount() == 2 && event.getButton().name().equals(MouseButton.PRIMARY.name())) {
                            //双击事件
                        }
                        if (event.getButton().name().equals(MouseButton.PRIMARY.name())) {
                            setChatWith(user);
                        } else if (event.getButton().name().equals(MouseButton.SECONDARY.name())) {
                            System.out.println(user.getAccount());
                            new FriendMenu(user).show(finalFriendPane1, Side.RIGHT,0,0);
                        }
                        ;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            friendPaneHolder.addFriendPane(user.getAccount(), friendPane);
            friendList.getChildren().add(friendPane);
        }
        ReceiveMessageService receiveMessageService = new ReceiveMessageService();
        receiveMessageService.start();

    }

    public void setChatWith(User user) {
        FriendPane friendPane = friendPaneHolder.getFriendPane(user.getAccount());
        if (user != currentChatWith) {
            this.currentChatWith = user;
            this.chatWith.setText(user.getNickName());
            friendPaneHolder.setCurrentChatUser(user);
            chatWith.setText(user.getNickName());
            friendPane.getChatRecordBox().heightProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    messageAreaScrollPane.setVvalue(1);
                }
            });
            messageAreaScrollPane.setContent(friendPane.getChatRecordBox());
        }
        friendPane.setUnReadMessageCountLabel(0);
    }

    public void sendMessage() throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        String date = dateFormat.format(now);
        String originMessage = messageEditArea.getText();
        String type = "[txt]";
        String message = MessageHandle.afterHandleMessage(type, user.getAccount(), currentChatWith.getAccount(), date,
                originMessage);
        dos.writeUTF(message);
        dos.flush();
        messageEditArea.clear();
        friendPaneHolder.getFriendPane(currentChatWith.getAccount()).getChatRecordBox().getChildren().add(new MessageCarrier(true, originMessage));
    }
}
