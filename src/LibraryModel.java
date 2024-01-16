/*
 * LibraryModel.java
 * Author:
 * Created on:
 */



import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LibraryModel {

    // For use in creating dialogs and making them modal
    private JFrame dialogParent;
    private Connection con = null;

    public LibraryModel(JFrame parent, String userid, String password) throws Exception {
	    dialogParent = parent;
        String url = "jdbc:postgresql://depot.ecs.vuw.ac.nz/" + userid + "_jdbc";

        Class.forName("org.postgresql.Driver");
        con = DriverManager.getConnection(url, userid,password);
    }

    public String bookLookup(int isbn) {
        String query = "SELECT * FROM Book Natural Join Book_Author Natural Join Author WHERE ISBN = ? ORDER BY authorid";

        try (PreparedStatement preparedStatement = con.prepareStatement(query)) {
            preparedStatement.setInt(1, isbn);

            ResultSet resultSet = preparedStatement.executeQuery();

            Book book = null;

            while (resultSet.next()){
                int bookIsbn = resultSet.getInt("isbn");
                String bookTitle = resultSet.getString("title");
                int numberOfCopies = resultSet.getInt("numofcop");
                int copiesLeft = resultSet.getInt("numleft");
                int editionNumber = resultSet.getInt("edition_no");
                String authorSurname = resultSet.getString("surname").trim();

                if(book == null) {
                    book = new Book(bookIsbn, bookTitle, numberOfCopies, copiesLeft, editionNumber, authorSurname);
                } else {
                    book.addAuthorSurname(authorSurname);
                }
            }

            if (book == null) {
                return String.format("Unable to locate a book with ISBN: %d", isbn);
            }

            return String.format(
                    "Book Lookup: \n" +
                            "\t ISBN: %d - Title: %s\n" +
                            "\t Edition: %d - Total Copies: %d - Available Copies: %d\n" +
                            "\t Authors: %s",
                    book.getIsbn(), book.getTitle().trim(),
                    book.getEditionNumber(), book.getNumberOfCopies(),
                    book.getCopiesLeft(), book.getAuthorSurnames());

        } catch (SQLException e) {
            return String.format("Encountered an SQL error while attempting to fetch book: %s", e.getMessage());
        }
    }


    public String showCatalogue() {
        String query = "SELECT * FROM Book Natural Join Book_Author Natural Join Author ORDER BY isbn";

        try (Statement statement = con.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);

            Map<Integer, Book> bookMap = new LinkedHashMap<>();

            while (resultSet.next()){
                int bookIsbn = resultSet.getInt("isbn");

                if (!bookMap.containsKey(bookIsbn)){
                    String bookTitle = resultSet.getString("title").trim();
                    int numberOfCopies = resultSet.getInt("numofcop");
                    int copiesLeft = resultSet.getInt("numleft");
                    int editionNumber = resultSet.getInt("edition_no");
                    String authorSurname = resultSet.getString("surname").trim();

                    Book book = new Book(bookIsbn, bookTitle, numberOfCopies, copiesLeft, editionNumber, authorSurname);
                    bookMap.put(bookIsbn, book);
                }
                else {
                    String authorSurname = resultSet.getString("surname").trim();
                    bookMap.get(bookIsbn).addAuthorSurname(authorSurname);
                }
            }

            StringBuilder catalogueBuilder = new StringBuilder("Catalogue Display: \n\n");

            for (Book book : bookMap.values()){
                catalogueBuilder
                        .append(String.format("ISBN: %d - Title: %s\n", book.getIsbn(), book.getTitle()))
                        .append(String.format("\t Edition: %d - Total Copies: %d - Available Copies: %d\n", book.getEditionNumber(), book.getNumberOfCopies(), book.getCopiesLeft()))
                        .append(String.format("\t Authors: %s\n", book.getAuthorSurnames()));
            }

            return catalogueBuilder.toString();
        } catch (SQLException e) {
            return String.format("SQL error encountered during catalogue display: %s", e.getMessage());
        }

    }

    public String showLoanedBooks() {
        String query = "SELECT * FROM Customer Natural Join Cust_Book Natural Join"
                + " Book Natural Join Book_Author Natural Join Author"
                + " ORDER BY customerid, isbn, authorid";

        try (Statement statement = con.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);

            StringBuilder loanedBooksBuilder = new StringBuilder("Display Loaned Books: \n\n");
            int previousCustomerId = -1;

            while (resultSet.next()) {
                int currentCustomerId = resultSet.getInt("customerid");

                if (currentCustomerId == previousCustomerId) {
                    continue;
                }

                int bookIsbn = resultSet.getInt("isbn");
                String bookTitle = resultSet.getString("title");
                int numberOfCopies = resultSet.getInt("numofcop");
                int copiesLeft = resultSet.getInt("numleft");
                int editionNumber = resultSet.getInt("edition_no");
                String lastName = resultSet.getString("l_name");
                String firstName = resultSet.getString("f_name");
                String city = resultSet.getString("city");
                List<String> authorSurnames = new ArrayList<>();

                authorSurnames.add(resultSet.getString("surname").trim());

                String authorList = String.join(", ", authorSurnames);

                loanedBooksBuilder.append(
                        String.format(
                                "\t ISBN: %d - Title: %s\n" +
                                        "\t Edition: %d - Total Copies: %d - Available Copies: %d\n" +
                                        "\t Authors: %s\n\t Borrowers:\n" +
                                        "\t\t Customer ID: %d - Name: %s, %s - City: %s\n",
                                bookIsbn, bookTitle.trim(),
                                editionNumber, numberOfCopies,
                                copiesLeft, authorList,
                                currentCustomerId, lastName.trim(),
                                firstName.trim(), city.trim())
                ).append("\n");

                previousCustomerId = currentCustomerId;
            }

            if(loanedBooksBuilder.toString().equals("Display Loaned Books: \n\n")) {
                return "No books are currently being loaned";
            }

            return loanedBooksBuilder.toString();
        } catch (SQLException e) {
            return String.format("Encountered an SQL error while attempting to display loaned books: %s", e.getMessage());
        }

    }



    public String showAuthor(int authorID) {

        String query = "SELECT * FROM Author Natural Join Book_Author Natural Join Book"
                + " WHERE authorid=" + authorID;

        try (Statement statement = con.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);

            String authorName = "";
            String authorSurname = "";
            List<String> bookDetails = new ArrayList<>();

            while (resultSet.next()){
                authorName = resultSet.getString("name").trim();
                authorSurname = resultSet.getString("surname").trim();
                int bookIsbn = resultSet.getInt("isbn");
                String bookTitle = resultSet.getString("title").trim();

                bookDetails.add(bookIsbn + " - " + bookTitle);
            }

            String formattedBookDetails = String.join("\n", bookDetails);

            return String.format(
                    "Display Author:\n%d - %s %s\nBooks written:\n%s",
                    authorID, authorName, authorSurname, formattedBookDetails);
        } catch (SQLException e) {
            return String.format("SQL error encountered while attempting to display author details: %s", e.getMessage());
        }

    }

    public String showAllAuthors() {

        String query = "SELECT * FROM Author";

        try (Statement statement = con.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);

            int authorId;
            String authorName;
            String authorSurname;

            StringBuilder resultBuilder = new StringBuilder("Display All Authors:\n");

            while (resultSet.next()){
                authorId = resultSet.getInt("authorid");
                authorName = resultSet.getString("name").trim();
                authorSurname = resultSet.getString("surname").trim();

                resultBuilder.append(String.format("\t%d: %s, %s\n", authorId, authorSurname, authorName));
            }

            return resultBuilder.toString();
        } catch (SQLException e) {
            return String.format("SQL error encountered while attempting to display all authors: %s", e.getMessage());
        }
    }

    public String showCustomer(int customerID) {
        String query = "SELECT * FROM customer WHERE customerid = " + customerID;

        try (Statement statement = con.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);

            int retrievedId = -1;
            String firstName = "";
            String lastName = "";
            String city = "";

            while (resultSet.next()){
                retrievedId = resultSet.getInt("customerid");
                firstName = resultSet.getString("f_name").trim();
                lastName = resultSet.getString("l_name").trim();
                city = resultSet.getString("city").trim();
            }

            String customerInfo = String.format("No customer with the ID %d exists.", customerID);

            if(retrievedId != -1) {
                customerInfo = String.format("Display Customer: \n\t%d: %s, %s - %s\n", retrievedId, lastName, firstName, city);
                String borrowedBooks = retrieveBorrowedBooks(customerID);
                customerInfo += String.format("\t%s", borrowedBooks);
            }

            return customerInfo;
        } catch (SQLException e) {
            return String.format("Encountered an SQL error: %s", e.getMessage());
        }

    }

    public String retrieveBorrowedBooks(int customerId) {
        String queryBooks = "SELECT isbn, title FROM cust_book Natural Join book WHERE customerID = " + customerId;

        try (Statement statement = con.createStatement()) {
            ResultSet resultSet = statement.executeQuery(queryBooks);

            StringBuilder borrowedBooksBuilder = new StringBuilder("Books Borrowed:");
            int bookCounter = 0;

            while(resultSet.next()){
                String isbn = resultSet.getString("isbn");
                String title = resultSet.getString("title");

                borrowedBooksBuilder.append(String.format("\n\t\t%s - %s", isbn, title));
                bookCounter++;
            }

            if(bookCounter > 0) {
                return borrowedBooksBuilder.toString();
            } else {
                return "(No books currently borrowed)";
            }
        } catch (SQLException e) {
            return String.format("Encountered an SQL error: %s", e.getMessage());
        }
    }


    public String showAllCustomers() {
        String query = "SELECT * FROM Customer";

        try (Statement statement = con.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);

            StringBuilder customerListBuilder = new StringBuilder("Display All Customers:\n");

            while (resultSet.next()) {
                int customerId = resultSet.getInt("customerid");
                String lastName = resultSet.getString("l_name").trim();
                String firstName = resultSet.getString("f_name").trim();
                String city = resultSet.getString("city");
                city = (city == null) ? "(No city listed)" : city.trim();

                customerListBuilder.append(String.format("\t%d: %s, %s - %s\n", customerId, lastName, firstName, city));
            }

            return customerListBuilder.toString();
        } catch (SQLException e) {
            return String.format("Encountered an SQL error: %s", e.getMessage());
        }
    }

    public String borrowBook(int isbn, int customerID,
			     int day, int month, int year) {
        String errorResponse = null;
        String date = year + "-" + month + "-" + day;

        String customerExistenceCheck = showCustomer(customerID);
        if (customerExistenceCheck.startsWith("Customer with id")) {
            return customerExistenceCheck;
        }

        int initialTransactionIsolationLevel = 0;
        try {
            initialTransactionIsolationLevel = con.getTransactionIsolation();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        String bookTitle = "";
        String customerFirstName = "";
        String customerLastName = "";
        int remainingBooks = -1;

        try {
            con.setAutoCommit(false);
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            String bookAndCustomerQuery = "SELECT title, numLeft, f_name, l_name from book Natural Join customer where isbn=" + isbn + " AND customerid=" + customerID;

            try (PreparedStatement selectStmt = con.prepareStatement(bookAndCustomerQuery)) {
                ResultSet resultSet = selectStmt.executeQuery();

                while (resultSet.next()) {
                    remainingBooks = resultSet.getInt("numLeft");
                    bookTitle = resultSet.getString("title").trim();
                    customerLastName = resultSet.getString("l_name").trim();
                    customerFirstName = resultSet.getString("f_name").trim();
                }
            }

            if (remainingBooks < 1) {
                throw new RuntimeException(String.format("Loan Book: \n\tNo copies available for book %d", isbn));
            }
            remainingBooks--;

            String insertQuery = "INSERT INTO cust_book VALUES (" + isbn + ",'" + date + "'," + customerID + ")";
            try (PreparedStatement insertStmt = con.prepareStatement(insertQuery)) {
                int rowsAffected = insertStmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new RuntimeException("Loan Book: \n\tBook loan unsuccessful.");
                }
            }

            String updateQuery = "UPDATE book set numLeft=" + remainingBooks + " where isbn = " + isbn;
            try (PreparedStatement updateStmt = con.prepareStatement(updateQuery)) {
                int rowsAffected = updateStmt.executeUpdate();

                if (rowsAffected == 0) {
                    throw new RuntimeException("Failed to decrease the number of available copies.");
                } else {
                    errorResponse = String.format("Book loan successful.\n\tBook: (%s)\n\tLoaned To: (%s %s)\n\tDue date: (%d %d %d)\n\t%d copies remaining.", bookTitle, customerFirstName, customerLastName, day, month, year, remainingBooks);
                }
            }
            con.commit();

        } catch (Exception exception) {
            errorResponse = handleBorrowBookException(exception, isbn);
            try {
                con.rollback();
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
                errorResponse += "\n Rollback unsuccessful.";
            }
        } finally {
            resetConnection(initialTransactionIsolationLevel);
        }

        return errorResponse;
    }

    private String handleBorrowBookException(Exception exception, int isbn) {
        String errorMessage = exception.getMessage().trim();
        if (errorMessage.startsWith("ERROR: duplicate key value violates unique constraint \"cust_book_pkey\"")) {
            return String.format("Customer already has a copy of the book with ISBN %d.", isbn);
        } else if (errorMessage.startsWith("ERROR: insert or update on table \"cust_book\" violates foreign key constraint")) {
            return String.format("Book with ISBN %d does not exist in our records.", isbn);
        } else {
            exception.printStackTrace();
            return errorMessage;
        }

    }

    private void resetConnection(int originalTransactionIsolationLevel) {
        try {
            con.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            con.setTransactionIsolation(originalTransactionIsolationLevel);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String returnBook(int isbn, int customerid) {
        String errorMessage = null;

        String customerExistenceCheck = showCustomer(customerid);
        if (customerExistenceCheck.startsWith("Customer with id")) {
            return customerExistenceCheck;
        }

        int originalTransactionIsolationLevel = 0;
        try {
            originalTransactionIsolationLevel = con.getTransactionIsolation();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        int bookCountLeft = -1;

        try {
            con.setAutoCommit(false);
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            String deleteQuery = "DELETE FROM cust_book WHERE isbn=" + isbn + " AND customerid=" + customerid;

            try (PreparedStatement deleteStmt = con.prepareStatement(deleteQuery)) {
                int rowsAffected = deleteStmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new RuntimeException(String.format("Return Book operation failed: \n\tThe customer with ID %d hasn't borrowed the book with ISBN %d.", customerid, isbn));
                }
            }

            String bookCountQuery = "SELECT numLeft from book where isbn= " + isbn;
            try (PreparedStatement selectStmt = con.prepareStatement(bookCountQuery)) {
                ResultSet resultSet = selectStmt.executeQuery();

                while (resultSet.next()) {
                    bookCountLeft = resultSet.getInt("numLeft");
                }
            }
            bookCountLeft++;

            String updateQuery = "UPDATE book set numLeft=" + bookCountLeft + " where isbn = " + isbn;
            try (PreparedStatement updateStmt = con.prepareStatement(updateQuery)) {
                int rowsAffected = updateStmt.executeUpdate();

                if (rowsAffected == 0) {
                    throw new RuntimeException(String.format("Return Book operation failed: \n\tUnable to increment the number of available copies of the book with ISBN %d.", isbn));
                } else {
                    errorMessage = String.format("Successfully returned the book with ISBN %d from customer with ID %d.\n\tNumber of available copies: %d", isbn, customerid, bookCountLeft);
                }
            }
            con.commit();

        } catch (Exception exception) {
            errorMessage = handleReturnBookException(exception);
            try {
                con.rollback();
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
                errorMessage += "\n Rollback operation failed.";
            }
        } finally {
            resetConnection(originalTransactionIsolationLevel);
        }

        return errorMessage;
    }

    private String handleReturnBookException(Exception exception) {
        String message = exception.getMessage().trim();
        exception.printStackTrace();
        return message;
    }

    public void closeDBConnection() {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException e) {
            System.err.println("Failed to close the database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String deleteCus(int customerID) {
        String deleteCustomerQuery = "DELETE FROM customer WHERE customerid = " + customerID;
        String deleteCustomerBookQuery = "DELETE FROM cust_book WHERE customerid = " + customerID;

        try {
            Statement statement = con.createStatement();
            statement.executeUpdate(deleteCustomerBookQuery);
            statement.executeUpdate(deleteCustomerQuery);

            return "Deleted customer " + customerID;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "Customer could not be deleted!";
    }

    public String deleteAuthor(int authorID) {
        String deleteAuthorQuery = "DELETE FROM author WHERE authorid = " + authorID;
        String deleteBookAuthorQuery = "DELETE FROM book_author WHERE authorid = " + authorID;

        try {
            Statement statement = con.createStatement();
            statement.executeUpdate(deleteBookAuthorQuery);
            statement.executeUpdate(deleteAuthorQuery);

            return "Deleted author " + authorID;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "Author could not be deleted!";
    }

    public String deleteBook(int isbn) {
        String deleteBookQuery = "DELETE FROM book WHERE isbn = " + isbn;
        String deleteBookAuthorQuery = "DELETE FROM book_author WHERE isbn = " + isbn;
        String deleteCustomerBookQuery = "DELETE FROM cust_book WHERE isbn = " + isbn;

        try {
            Statement statement = con.createStatement();
            statement.executeUpdate(deleteBookAuthorQuery);
            statement.executeUpdate(deleteCustomerBookQuery);
            statement.executeUpdate(deleteBookQuery);

            return "Deleted book " + isbn;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "Book could not be deleted!";
    }
}