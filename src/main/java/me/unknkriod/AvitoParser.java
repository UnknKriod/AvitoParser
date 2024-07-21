package me.unknkriod;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AvitoParser {
    public static void main(String[] args) {
        List<Map<String, Integer>> correctItems = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);
        System.out.print("Введите название товара: ");
        String prompt = scanner.nextLine().trim();
        scanner.close();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Подходящие результаты:");
            for (Map<String, Integer> item : correctItems) {
                String avitoUrl = "";
                int avitoPrice = 0;
                String yandexUrl = "";
                int yandexPrice = 0;
                for (Map.Entry<String, Integer> entry : item.entrySet()) {
                    if (entry.getKey().startsWith("https://www.avito.ru/")) {
                        avitoUrl = entry.getKey();
                        avitoPrice = entry.getValue();
                    } else {
                        yandexUrl = entry.getKey();
                        yandexPrice = entry.getValue();
                    }
                }
                System.out.println(avitoUrl + " " + avitoPrice + " рублей - " + yandexUrl + " " + yandexPrice + " рублей");
            }
        }));

        try {
            String avitoUrl = "https://www.avito.ru/";
            // Кодируем регион, чтобы добавить его в URL-адрес
            String region = "respublika_krym";
            String encodedRegion = URLEncoder.encode(region, "UTF-8");
            String searchUrl = avitoUrl + encodedRegion + "?q=" + URLEncoder.encode(prompt, "UTF-8");

            Document avitoDoc = Jsoup.connect(searchUrl).get();

            // Получаем название и цену из HTML-кода Avito
            Elements titleElements = avitoDoc.select("h3[itemprop=name]");
            Elements urlElements = avitoDoc.select("a[itemprop=url]");
            //Elements priceElements = avitoDoc.select(".price-price-JP7qe");

            // Перебираем все объявления на Avito
            for (int i = 0; i < titleElements.size(); i++) {
                Element titleElement = titleElements.get(i);
                //Element priceElement = priceElements.get(i);
                Element urlElement = urlElements.get(i);

                String avitoLink = avitoUrl + urlElement.attr("href").substring(1);
                String title = titleElement.text();
                //String priceText = priceElement.selectFirst("meta[itemprop=price]").attr("content");
                //int priceAvito = Integer.parseInt(priceText);
                // Переходим по ссылке на объявление на Avito
                Document avitoPageDoc = Jsoup.connect(avitoLink).get();

                // Находим элемент, содержащий цену (здесь приведен пример)
                String priceText = avitoPageDoc.selectFirst("span[itemprop=price]").attr("content");

                // Преобразуем строку в целое число (цену)
                int priceAvito = Integer.parseInt(priceText);

                // Теперь у нас есть название и цена текущего объявления на Avito
                System.out.println("Название товара на Avito: " + title);
                System.out.println("Цена товара на Avito: " + priceAvito);
                System.out.println("Ссылка на товар: " + avitoLink);

                try {
                    // Ищем название товара в поисковике Яндекса
                    String yandexUrl = "https://yandex.ru/search/?text=";
                    Document yandexDoc = Jsoup.connect(yandexUrl + URLEncoder.encode(title + " site:market.yandex.ru", "UTF-8")).get();

                    // Получаем все ссылки из результатов поиска на Яндексе
                    Elements links = yandexDoc.select(".organic .link");

                    for (Element link : links) {
                        String url = link.attr("href");
                        if (url.contains("market.yandex.ru")) {
                            Document pageDoc = Jsoup.connect(url).get();

                            // Проверяем наличие цены на странице
                            Elements priceValues = pageDoc.select("[data-auto=snippet-price-current]");
                            if (!priceValues.isEmpty()) {
                                String priceTextOnPage = priceValues.get(0).text().replaceAll("\\D", ""); // Удаляем все символы, кроме цифр
                                int price = Integer.parseInt(priceTextOnPage);
                                System.out.println("Цена товара на странице " + url + ": " + price + " рублей");

                                if (price != 0 && price * 0.8 > priceAvito) {
                                    System.out.println("Подходящее объявление! " + avitoLink + " цена: " + priceAvito);
                                    Map<String, Integer> item = new HashMap<>();
                                    item.put(avitoLink, priceAvito);
                                    item.put(url, price);
                                    correctItems.add(item);
                                } else {
                                    System.out.println("Ничего не найдено!");
                                }
                                break; // Выходим из цикла после нахождения цены на Яндекс.Маркете
                            } else {
                                Elements snippetPriceValues = pageDoc.select(".snippet-price-value");
                                if (!snippetPriceValues.isEmpty()) {
                                    String priceTextOnPage = snippetPriceValues.get(0).text().replaceAll("\\D", ""); // Удаляем все символы, кроме цифр
                                    int price = Integer.parseInt(priceTextOnPage);
                                    System.out.println("Цена товара на странице " + url + ": " + price + " рублей");

                                    if (price != 0 && price * 0.8 > priceAvito) {
                                        System.out.println("Подходящее объявление! " + avitoLink + " цена на Авито: " + priceAvito + " цена на Яндекс Маркете: " + price);
                                        Map<String, Integer> item = new HashMap<>();
                                        item.put(avitoLink, priceAvito);
                                        item.put(url, price);
                                        correctItems.add(item);
                                    }
                                    break; // Выходим из цикла после нахождения цены на Яндекс.Маркете
                                } /*else {
                                    System.out.println("Цена товара не найдена на странице " + url);
                                }*/
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Перебор закончился!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
