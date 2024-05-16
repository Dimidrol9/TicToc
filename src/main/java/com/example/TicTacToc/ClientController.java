package com.example.TicTacToc;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;


public class ClientController implements Initializable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private char mark;
    private Map<Integer, Button> boardButtons;
    public MusicPlayer musicPlayer;

    @FXML
    private TextField textFieldIP;//Граце водить свій IP
    @FXML
    private Text clientText;//Відображення тексту , про хід гравців , яку стані зараз зхадодиться гравець
    @FXML
    private TextField textFieldPort;//Порт для підключення на сервер
    @FXML
    private Button buttonConnect;
    @FXML
    private Text textStatus;//Текст що все добре запущено і тд.
    @FXML
    private Button button0, button1, button2, button3, button4, button5, button6, button7, button8;
    @FXML
    private Button buttonReset;
    @FXML
    private Text redText;//Текст для проблем , із запущенням і тд.

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        musicPlayer = new MusicPlayer();
        boardButtons = new HashMap<>();
        boardButtons.put(0, button0);
        boardButtons.put(1, button1);
        boardButtons.put(2, button2);
        boardButtons.put(3, button3);
        boardButtons.put(4, button4);
        boardButtons.put(5, button5);
        boardButtons.put(6, button6);
        boardButtons.put(7, button7);
        boardButtons.put(8, button8);
        for (int i = 0; i < 9; i++) {
            Button button = boardButtons.get(i);
            int Key = i;
            button.setOnAction(actionEvent -> {
                try {
                    out.println("MOVE " + Key);
                    System.out.println("MOVE: " + Key);
                } catch (Exception e) {
                    redText.setText("Помилка в ході гравця: " + e.getMessage());
                }
            });
        }
        buttonConnect.setOnAction(event -> {
            String serverAddress = textFieldIP.getText();
            int port = Integer.parseInt(textFieldPort.getText());
            try {
                connectToServer(serverAddress, port);
                musicPlayer.playMusic("/music/background.mp3");
                textStatus.setText("Підключення клієнта:"+serverAddress+":"+port);
            } catch (Exception e) {
                redText.setText("Помилка підключення: " + e.getMessage());
            }
        });
        buttonReset.setOnAction(actionEvent -> resetBoard());
    }

    private void connectToServer(String serverAddress, int port) throws Exception {
        socket = new Socket(serverAddress, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        String response = in.readLine();
        if (response != null && response.startsWith("WELCOME")) {
            mark = response.charAt(8);
            System.out.println("Підключення гравця " + mark);

            new Thread(()->{
            try {
                    play();
                } catch (Exception e) {
                    redText.setText("Помилка: " + e.getMessage());
                }
            }).start();
        } else {
            redText.setText("Сервер відхилив підключення");
        }
    }

    private void play() throws IOException {
        String response;


        boolean shouldQuit = false;
        while (!shouldQuit && (response = in.readLine()) != null) {
            String finalResponse = response;
            Platform.runLater(() -> {
            if (finalResponse.startsWith("VALID_MOVE") && finalResponse.length() > 11) {
                int loc = Integer.parseInt(finalResponse.substring(11));
                if (boardButtons.containsKey(loc)) {
                    Button button = boardButtons.get(loc);
                    button.setText(String.valueOf(mark));
                    button.setFont(Font.font(20));
                    } else {
                    clientText.setText("Отримано недійсний хід від сервера");
                }
            } else if (finalResponse.startsWith("OPPONENT_MOVED")) {
                String[] parts = finalResponse.split(" ");
                int loc = Integer.parseInt(parts[1]);
                char opponentMark = parts[2].charAt(0);
                if (boardButtons.containsKey(loc)) {
                    Button button = boardButtons.get(loc);
                    button.setText(String.valueOf(opponentMark));
                    button.setFont(Font.font(20));
                } else {
                    clientText.setText("Отримано недійсний хід від сервера");
                }

            }else if (finalResponse.startsWith("YOUR_TURN")) {
                clientText.setText(finalResponse.substring(10));
            }else if(finalResponse.startsWith("OPPONENT_TURN")) {
                clientText.setText(finalResponse.substring(13));
            }else if (finalResponse.startsWith("VICTORY")) {
                clientText.setText(finalResponse.substring(8));
                musicPlayer.playSound("/sound/win.mp3");
            } else if (finalResponse.startsWith("DEFEAT")) {
                clientText.setText(finalResponse.substring(8));
                musicPlayer.playSound("/sound/lose.mp3");
            } else if (finalResponse.startsWith("TIE")) {
                clientText.setText(finalResponse.substring(3));
                musicPlayer.playSound("sound/tie.mp3");
            } else if (finalResponse.startsWith("MESSAGE")) {
                clientText.setText(finalResponse.substring(8));
            }
            else if (finalResponse.startsWith("BOARD")) {
                String boardState = finalResponse.substring(6);
                for (int i = 0; i < boardState.length(); i++) {
                    char symbol = boardState.charAt(i);
                    if (symbol != '-') {
                        Button button = boardButtons.get(i);
                        button.setText(String.valueOf(symbol));
                        button.setFont(Font.font(20));
                    }
                }
            }

        });
        }

        out.println("QUIT");
        socket.close();
    }

    private void resetBoard() {
        for (Button button : boardButtons.values()) {
            button.setText("");
        }
        clientText.setText("");
        try {
            out.println("RESET");
            musicPlayer.stopMusic();
            musicPlayer.playMusic("/music/background.mp3");
        } catch (Exception e) {
            redText.setText("Помилка скидання гри: " + e.getMessage());
        }
    }
}