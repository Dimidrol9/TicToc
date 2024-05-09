package com.example.TicTacToc;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

public class ServerController implements Initializable {

    @FXML
    private ListView<String> listClient;

    @FXML
    private Button buttonStart;
    @FXML
    private Button buttonStop;
    @FXML
    private TextField textFieldPort;
    @FXML
    private Text greenText;
    @FXML
    private Text redText;

    private ServerSocket serverSocket;
    private ArrayList<Game.Player> players;

    public Socket currentPlayerSocket;
    public char currentPlayerSymbol = 'X';

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize the board
        buttonStop.setOnAction(actionEvent -> handleStopButton());
        buttonStart.setOnAction(actionEvent -> handleStartButton());
    }

    @FXML
    private void handleStartButton() {
        try {
            int port = Integer.parseInt(textFieldPort.getText());
            startServer(port);
        } catch (Exception e) {
            redText.setText("Помилка підключення:зайнятий порт ::" + e.getMessage());
        }
    }

    @FXML
    private void handleStopButton() {
        stopServer();
    }

    private void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            greenText.setText("Сервер запущений на порту: " + port);
            players = new ArrayList<>();

            new Thread(() -> {
                try {
                    while (true) {
                        Socket socket = serverSocket.accept();
                        handleClient(socket);
                    }
                } catch (IOException e) {
                    redText.setText("Помилка сервера: " + e.getMessage());
                }
            }).start();
        } catch (IOException e) {
            redText.setText("Помилка запуску сервера: " + e.getMessage());
        }
    }

    private void handleClient(Socket socket) {
        try {
            Game game = new Game();
            Game.Player playerX = game.new Player(socket, 'X');
            Game.Player playerO = game.new Player(serverSocket.accept(), 'O');
            playerX.setOpponent(playerO);
            playerO.setOpponent(playerX);
            game.currentPlayer = playerX;

            Platform.runLater(() -> {
                listClient.getItems().add("Гравець X: " + playerX.socket.getRemoteSocketAddress());
                listClient.getItems().add("Гравець O: " + playerO.socket.getRemoteSocketAddress());
            });

            players.add(playerX);
            players.add(playerO);
            playerX.start();
            playerO.start();
        } catch (IOException e) {
            redText.setText("Помилка обробки клієнта: " + e.getMessage());
        }
    }

    private void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                greenText.setText("Сервер зупинився");
                for (Game.Player player : players) {
                    player.interrupt();
                }
                players.clear();
                listClient.getItems().clear();
            }
        } catch (IOException e) {
            redText.setText("Помилка запущення сервера: " + e.getMessage());
        }
    }

    class Game {
        public final Player[] board =
                {
                null, null, null,
                null, null, null,
                null, null, null
                };
        Player currentPlayer;

        public boolean hasWinner() {
            return
                    (board[0] != null && board[0] == board[1] && board[0] == board[2])
                            ||(board[3] != null && board[3] == board[4] && board[3] == board[5])
                            ||(board[6] != null && board[6] == board[7] && board[6] == board[8])
                            ||(board[0] != null && board[0] == board[3] && board[0] == board[6])
                            ||(board[1] != null && board[1] == board[4] && board[1] == board[7])
                            ||(board[2] != null && board[2] == board[5] && board[2] == board[8])
                            ||(board[0] != null && board[0] == board[4] && board[0] == board[8])
                            ||(board[2] != null && board[2] == board[4] && board[2] == board[6]);
        }

        public boolean boardFilledUp() {
            for (Player player : board) {
                if (player == null) {
                    return false;
                }
            }
            return true;
        }
        private void updateBoardForBothPlayers() {
            StringBuilder boardState = new StringBuilder();
            for (Player p : board) {
                boardState.append(p == null ? "-" : p.mark);//тернарним оператором.
            }
            for (Player player : players) {
                player.output.println("BOARD " + boardState);
            }
        }

        public synchronized boolean legalMove(int location, Player player) {
            if (player == currentPlayer && board[location] == null) {
                board[location] = currentPlayer;
                updateBoardForBothPlayers();
                currentPlayer.output.println("OPPONENT_TURN Хід противника");
                currentPlayer.opponentMoved(location);
                if (hasWinner()) {
                    currentPlayer.output.println("VICTORY Ти виграв");
                    currentPlayer.opponent.output.println("DEFEAT  Ти програв");
                } else if (boardFilledUp()) {
                    currentPlayer.output.println("TIE Нічия");
                    currentPlayer.opponent.output.println("TIE Нічия");
                } else {
                    currentPlayer = currentPlayer.opponent;
                    currentPlayer.output.println("YOUR_TURN Ваш хід");
                }
                return true;
            }
            return false;
        }

        class Player extends Thread {
            char mark;
            Player opponent;
            Socket socket;
            BufferedReader input;
            PrintWriter output;

            public Player(Socket socket, char mark) {
                this.socket = socket;
                this.mark = mark;
                try {
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    output = new PrintWriter(socket.getOutputStream(), true);
                    output.println("WELCOME " + mark);
                    System.out.println("Ласкаво просимо " + mark);
                    output.println("MESSAGE Чекаємо зєднання гравців");
                } catch (IOException e) {
                    System.out.println("Гравець помер: " + e);
                }
            }

            public void setOpponent(Player opponent) {
                this.opponent = opponent;
            }

            public void opponentMoved(int location) {
                output.println("OPPONENT_MOVED " + location + " " + opponent.mark);
            }


            public void run() {
                try {
                    output.println("MESSAGE Всі гравці підключенні");

                    if (mark == currentPlayerSymbol) {
                        output.println("MESSAGE  Ваш хід");
                        currentPlayerSocket = socket;
                    }


                    while (true) {
                        String command = input.readLine();
                        if (command.startsWith("MOVE") && command.length() > 5) {
                            int location = Integer.parseInt(command.substring(5));
                            if (legalMove(location, this)) {
                                output.println("VALID_MOVE " + location );
                            } else {
                                output.println("MESSAGE Неправильний хід , спробуйте ще раз");
                            }
                        } else if (command.startsWith("QUIT")) {
                            return;
                        }else if (command.startsWith("RESET")) {
                            Arrays.fill(board, null);
                            currentPlayer = (currentPlayerSymbol == 'X') ? this : opponent;
                            output.println("MESSAGE ПЕРЕЗАПУСК");
                        }
                        else {
                            output.println("MESSAGE Неправильна команда");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Гравець помер: " + e);
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        redText.setText("Помилка "+e.getMessage());
                    }

                }
            }
        }
    }
}