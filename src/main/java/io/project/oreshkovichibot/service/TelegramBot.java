package io.project.oreshkovichibot.service;

import io.project.oreshkovichibot.config.OreshkovichiBotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    final OreshkovichiBotConfig config;

    public TelegramBot(OreshkovichiBotConfig config) {
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return this.config.getBotName();
    }

    @Override
    public String getBotToken() {
        return this.config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            switch (message) {
                case "/start":
                    startCommandReceive(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/temperature":
                    temperatureCommandReceive(chatId);
                    break;
                case "/humidity":
                    humidityCommandReceive(chatId);
                    break;
                case "/pressure":
                    pressureCommandReceive(chatId);
                    break;
                case "/faq":
                    sendFAQ(chatId);
                    break;
                case "/all":
                    allCommandReceive(chatId);
                    break;
                default:
                    if (message.matches("/temperature \\d+")) {
                        temperatureSendMessage(chatId, Integer.parseInt(message.replaceAll("\\D", "")));
                        break;
                    }
                    else if (message.matches("/humidity \\d+")) {
                        humiditySendMessage(chatId, Integer.parseInt(message.replaceAll("\\D", "")));
                        break;
                    }
                    else if (message.matches("/pressure \\d+")) {
                        pressureSendMessage(chatId, Integer.parseInt(message.replaceAll("\\D", "")));
                        break;
                    }
                    sendMessage(chatId, "Неизвестная команда..");
                    break;
            }
        }
    }

    private void sendFAQ(long chatId) {
        sendMessage(chatId,
                "/temperature - последние 10 показаний температуры\n" +
                "/humidity - последние 10 показаний влажности\n" +
                "/pressure - последние 10 показаний давления\n" +
                "/all - последние показания всех величин\n" +
                "/temperature *number* - последние *number* показаний температуры\n" +
                "/faq - справка\n");
    }

    private void startCommandReceive(long chatId, String userName) {
        sendMessage(chatId, "Привет, " + userName + ", вот что я умею..");
        sendFAQ(chatId);
    }

    private void sendMessage(long chatId, String textMessage) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textMessage);

        try {
            execute(message);
        }
        catch (TelegramApiException telegramApiException) {
            ///
        }
    }

    private void temperatureCommandReceive (long chatId) {
        temperatureSendMessage(chatId, 10);
    }

    private void temperatureSendMessage(long chatId, int amount) {
        try {
            URL url = new URL("https://thingspeak.com/channels/2013222/field/1.json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() != 200)
                throw new RuntimeException("ResponseCode: " + connection.getResponseCode());
            else {
                ArrayList<String> temperatureList = getParameter(url, "field1");
                ArrayList<String> dateList = getParameter(url, "created_at");
                if (amount > dateList.size()) amount = temperatureList.size();
                int count = 1; StringBuilder stringBuilder = new StringBuilder();
                for (int i = temperatureList.size() - amount; i < temperatureList.size(); i++) {
                    stringBuilder.append(count + ". " + "Температура: " + Double.parseDouble(temperatureList.get(i)) + " °C" +
                            "\nДата измерения: " + dateList.get(i).replaceAll("T", " ").replaceAll("Z", " ") + "\n\n");
                    if (count == 50) {
                        sendMessage(chatId, String.valueOf(stringBuilder));
                        stringBuilder = new StringBuilder();
                    }
                    count++;
                }
                sendMessage(chatId, String.valueOf(stringBuilder));
            }
        } catch (IOException malformedURLException) {
            throw new RuntimeException(malformedURLException);
        }
    }

    private void humidityCommandReceive(long chatId) {
        humiditySendMessage(chatId, 10);
    }

    private void humiditySendMessage(long chatId, int amount) {
        try {
            URL url = new URL("https://thingspeak.com/channels/2013222/field/2.json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() != 200)
                throw new RuntimeException("ResponseCode: " + connection.getResponseCode());
            else {
                ArrayList<String> humidityList = getParameter(url, "field2");
                ArrayList<String> dateList = getParameter(url, "created_at");
                if (amount > dateList.size()) amount = dateList.size();
                int count = 1; StringBuilder stringBuilder = new StringBuilder();
                for (int i = humidityList.size() - amount; i < humidityList.size(); i++) {
                    stringBuilder.append(count + ". " + "Влажность: " + humidityList.get(i) + "%" +
                            "\nДата измерения: " + dateList.get(i).replaceAll("T", " ").replaceAll("Z", " ") + "\n\n");
                    count++;
                }
                sendMessage(chatId, String.valueOf(stringBuilder));
            }
        } catch (IOException malformedURLException) {
            throw new RuntimeException(malformedURLException);
        }
    }

    private void pressureCommandReceive(long chatId) {
        pressureSendMessage(chatId, 10);
    }

    private void pressureSendMessage(long chatId, int amount) {
        try {
            URL url = new URL("https://thingspeak.com/channels/2013222/field/3.json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() != 200)
                throw new RuntimeException("ResponseCode: " + connection.getResponseCode());
            else {
                ArrayList<String> pressureList = getParameter(url, "field3");
                ArrayList<String> dateList = getParameter(url, "created_at");
                if (amount > dateList.size()) amount = dateList.size();
                int count = 1; StringBuilder stringBuilder = new StringBuilder();
                for (int i = pressureList.size() - amount; i < pressureList.size(); i++) {
                    stringBuilder.append(count + ". " + "Давление: " + pressureList.get(i) + " HPA" +
                            "\nДата измерения: " + dateList.get(i).replaceAll("T", " ").replaceAll("Z", " ") + "\n\n");
                    count++;
                }
                sendMessage(chatId, String.valueOf(stringBuilder));
            }
        } catch (MalformedURLException malformedURLException) {
            throw new RuntimeException(malformedURLException);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }



    private ArrayList<String> getParameter(URL url, String field) throws IOException {
        ArrayList<String> parameterString = new ArrayList<>();
        org.json.JSONObject obj = new org.json.JSONObject(getJSONString(url));
        org.json.JSONArray arr = obj.getJSONArray("feeds");
        for (int i = 0; i < arr.length(); i++) {
            parameterString.add(arr.getJSONObject(i).getString(field));
        }
        return parameterString;
    }

    private void allCommandReceive(long chatId) {
        try {
            URL url = new URL("https://api.thingspeak.com/channels/2013222/feeds.json?results=1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            sendMessage(chatId,
                    "Температура: " + oneTemperatureCommandReceive() + " °C"
                            + "\nВлажность: " + oneHumidityCommandReceive() + "%"
                            + "\nДавление: " + onePressureCommandReceive() + " HPA"
                            + "\nДата измерения: " + getUpdateDate());
        }
        catch (IOException ioException) {
            throw new RuntimeException(ioException.getMessage());
        }
    }

    private String oneTemperatureCommandReceive() {
        try {
            URL url = new URL("https://api.thingspeak.com/channels/2013222/feeds.json?results=1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() != 200)
                throw new RuntimeException("ResponseCode: " + connection.getResponseCode());
            else {
                return String.valueOf(Double.parseDouble(getOneParameter(url, "field1")));
            }
        } catch (MalformedURLException malformedURLException) {
            throw new RuntimeException(malformedURLException);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private String oneHumidityCommandReceive() {
        try {
            URL url = new URL("https://api.thingspeak.com/channels/2013222/feeds.json?results=1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() != 200)
                throw new RuntimeException("ResponseCode: " + connection.getResponseCode());
            else {
                return getOneParameter(url, "field2");
            }
        } catch (MalformedURLException malformedURLException) {
            throw new RuntimeException(malformedURLException);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private String onePressureCommandReceive() {
        try {
            URL url = new URL("https://api.thingspeak.com/channels/2013222/feeds.json?results=1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() != 200)
                throw new RuntimeException("ResponseCode: " + connection.getResponseCode());
            else {
                return getOneParameter(url, "field3");
            }
        } catch (MalformedURLException malformedURLException) {
            throw new RuntimeException(malformedURLException);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private String getOneParameter(URL url, String field) throws IOException {
        org.json.JSONObject obj = new org.json.JSONObject(getJSONString(url));
        org.json.JSONArray arr = obj.getJSONArray("feeds");

        String parameter = null;
        for (int i = 0; i < arr.length(); i++) {
            parameter = arr.getJSONObject(i).getString(field);
        }
        return parameter;
    }

    private String getUpdateDate() throws IOException {
        try {
            URL url = new URL("https://api.thingspeak.com/channels/2013222/feeds.json?results=1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() != 200)
                throw new RuntimeException("ResponseCode: " + connection.getResponseCode());
            else {
                return getOneParameter(url, "created_at").replaceAll("T", " ").replaceAll("Z", " ");
            }
        } catch (MalformedURLException malformedURLException) {
            throw new RuntimeException(malformedURLException);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private String getJSONString(URL url) throws IOException {
        StringBuilder informationString = new StringBuilder();
        Scanner scanner = new Scanner(url.openStream());

        while (scanner.hasNext()) {
            informationString.append(scanner.nextLine());
        }

        scanner.close();

        return String.valueOf(informationString);
    }
}
