package Main;

import DAO.Books_rentalDAO;

import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Books_rentalDAO dao = new Books_rentalDAO();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n==== 도서 대여 관리 시스템 ====");
            System.out.println("1. 책 목록 조회");
            System.out.println("2. 책 검색");
            System.out.println("3. 책 대여");
            System.out.println("4. 책 반납");
            System.out.println("5. 회원별 대여 리스트 조회");
            System.out.println("0. 종료");
            System.out.print("메뉴 선택: ");

            int menu;
            try {
                menu = Integer.parseInt(sc.nextLine());
            } catch (Exception e) {
                System.out.println("잘못된 입력입니다.");
                continue;
            }

            switch (menu) {
                case 1: {
                    ArrayList<String> books = dao.getBooks();
                    System.out.println("\n=== 책 목록 ===");
                    if (books.isEmpty()) {
                        System.out.println("등록된 도서가 없습니다.");
                    } else {
                        for (String b : books) System.out.println(b);
                    }
                    break;
                }

                case 2: {
                    System.out.print("검색어 입력(제목 또는 작가): ");
                    String keyword = sc.nextLine();

                    ArrayList<String> result = dao.searchBooks(keyword);

                    System.out.println("\n=== 검색 결과 ===");
                    if (result.isEmpty()) {
                        System.out.println("검색 결과가 없습니다");
                    } else {
                        for (String r : result) System.out.println(r);
                    }
                    break;
                }

                case 3: {
                    System.out.print("회원 이름 입력: ");
                    String memberName = sc.nextLine();

                    System.out.print("대여할 책 이름 입력: ");
                    String bookTitle = sc.nextLine();

                    dao.rentBookByName(memberName, bookTitle);
                    break;
                }

                case 4: {
                    System.out.print("반납할 책 이름 입력: ");
                    String bookTitle = sc.nextLine();

                    dao.returnBookByTitle(bookTitle);
                    break;
                }

                case 5: {
                    System.out.print("회원 이름 입력: ");
                    String memberName = sc.nextLine();

                    ArrayList<String> rentals = dao.getRentalsByMemberName(memberName);

                    if (rentals.isEmpty()) {
                        System.out.println("해당 회원의 대여 기록이 없습니다");
                    } else {
                        System.out.println("\n=== 대여 내역 ===");
                        for (String s : rentals) System.out.println(s);
                    }
                    break;
                }

                case 0:
                    System.out.println("프로그램 종료");
                    return;

                default:
                    System.out.println("잘못된 입력입니다.");
                    break;
            }
        }
    }
}
