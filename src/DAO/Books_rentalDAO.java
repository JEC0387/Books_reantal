package DAO;

import DB.DBUtill;

import java.sql.*;
import java.util.ArrayList;

public class Books_rentalDAO {

    // 1) 책 목록 조회
    public ArrayList<String> getBooks() {
        ArrayList<String> list = new ArrayList<>();

        String sql = "SELECT book_id, title, author, status FROM books ORDER BY book_id";

        try (Connection conn = DBUtill.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String bookInfo = rs.getInt("book_id") + " - " +
                        rs.getString("title") + " / " +
                        rs.getString("author") + " / 상태: " +
                        rs.getString("status");
                list.add(bookInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // 2) 책 검색 (제목 또는 작가)
    public ArrayList<String> searchBooks(String keyword) {
        ArrayList<String> list = new ArrayList<>();

        String sql = "SELECT book_id, title, author, status FROM books " +
                "WHERE title LIKE ? OR author LIKE ? " +
                "ORDER BY book_id";

        try (Connection conn = DBUtill.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String info = rs.getInt("book_id") + " - " +
                            rs.getString("title") + " / " +
                            rs.getString("author") + " / 상태: " +
                            rs.getString("status");
                    list.add(info);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // 3) 책 대여 (회원 이름 + 책 이름)
    public boolean rentBookByName(String memberName, String bookTitle) {

        Integer memberId = getMemberIdByName(memberName);
        if (memberId == null) {
            System.out.println("등록되지 않은 사용자입니다.");
            return false;
        }

        Integer bookId = getBookIdByTitle(bookTitle);
        if (bookId == null) {
            System.out.println("도서가 없습니다.");
            return false;
        }

        if (!isBookAvailable(bookId)) {
            System.out.println("대여 실패!");
            return false;
        }

        String rentSql = "INSERT INTO rentals(member_id, book_id, due_date) " +
                "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 7 DAY))";
        String updateBookSql = "UPDATE books SET status='RENTED' WHERE book_id=?";

        try (Connection conn = DBUtill.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(rentSql)) {
                pstmt.setInt(1, memberId);
                pstmt.setInt(2, bookId);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updateBookSql)) {
                pstmt.setInt(1, bookId);
                pstmt.executeUpdate();
            }

            conn.commit();
            System.out.println("대여 성공!");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("대여 실패!");
            return false;
        }
    }

    // 4) 책 반납 (책 이름 입력)
    public boolean returnBookByTitle(String bookTitle) {

        // 책 존재 확인
        Integer bookId = getBookIdByTitle(bookTitle);
        if (bookId == null) {
            System.out.println("도서가 없습니다.");
            return false;
        }

        // 대여중인 rentals 찾기(가장 최근 RENTED 1건)
        String findRentalSql =
                "SELECT rental_id, due_date " +
                        "FROM rentals " +
                        "WHERE book_id=? AND return_status='RENTED' " +
                        "ORDER BY rental_date DESC " +
                        "LIMIT 1";

        String insertReturnSql = "INSERT INTO returns(rental_id, late_fee) VALUES(?, ?)";
        String updateRentalSql = "UPDATE rentals SET return_status='RETURNED', return_date=NOW() WHERE rental_id=?";
        String updateBookSql = "UPDATE books SET status='AVAILABLE' WHERE book_id=?";

        try (Connection conn = DBUtill.getConnection()) {
            conn.setAutoCommit(false);

            Integer rentalId = null;
            Date dueDate = null;

            try (PreparedStatement pstmt = conn.prepareStatement(findRentalSql)) {
                pstmt.setInt(1, bookId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        rentalId = rs.getInt("rental_id");
                        dueDate = rs.getDate("due_date");
                    }
                }
            }

            if (rentalId == null) {
                System.out.println("대여 중인 도서가 아닙니다");
                return false;
            }

            long lateFee = calcLateFee(dueDate);

            try (PreparedStatement pstmt = conn.prepareStatement(insertReturnSql)) {
                pstmt.setInt(1, rentalId);
                pstmt.setLong(2, lateFee);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updateRentalSql)) {
                pstmt.setInt(1, rentalId);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updateBookSql)) {
                pstmt.setInt(1, bookId);
                pstmt.executeUpdate();
            }

            conn.commit();
            System.out.println("반납 성공!");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("반납 실패!");
            return false;
        }
    }

    // 5) 회원별 대여 리스트 조회 (회원 이름 입력)
    public ArrayList<String> getRentalsByMemberName(String memberName) {
        ArrayList<String> list = new ArrayList<>();

        Integer memberId = getMemberIdByName(memberName);
        if (memberId == null) {
            System.out.println("등록되지 않은 사용자입니다.");
            return list; // 빈 리스트 반환
        }

        String sql =
                "SELECT r.rental_id, b.title, b.author, r.rental_date, r.due_date, r.return_status " +
                        "FROM rentals r " +
                        "JOIN books b ON r.book_id = b.book_id " +
                        "WHERE r.member_id=? " +
                        "ORDER BY r.rental_date DESC";

        try (Connection conn = DBUtill.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, memberId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String rentalInfo =
                            "대여번호: " + rs.getInt("rental_id") +
                                    " | 책: " + rs.getString("title") +
                                    " (" + rs.getString("author") + ")" +
                                    " | 대여일: " + rs.getTimestamp("rental_date") +
                                    " | 반납예정일: " + rs.getTimestamp("due_date") +
                                    " | 상태: " + rs.getString("return_status");
                    list.add(rentalInfo);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ----------------- 내부 헬퍼 -----------------

    private Integer getMemberIdByName(String memberName) {
        String sql = "SELECT member_id FROM members WHERE name = ? LIMIT 1";

        try (Connection conn = DBUtill.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, memberName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("member_id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Integer getBookIdByTitle(String title) {
        String sql = "SELECT book_id FROM books WHERE title = ? LIMIT 1";

        try (Connection conn = DBUtill.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("book_id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isBookAvailable(int bookId) {
        String sql = "SELECT status FROM books WHERE book_id = ?";

        try (Connection conn = DBUtill.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return "AVAILABLE".equals(rs.getString("status"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 연체료 계산 (하루 1000원)
    private long calcLateFee(Date dueDate) {
        long feePerDay = 1000;

        Date today = new Date(System.currentTimeMillis());
        long diff = today.getTime() - dueDate.getTime();
        long daysLate = diff / (1000L * 60 * 60 * 24);

        return (daysLate > 0) ? daysLate * feePerDay : 0;
    }
}
