class Book {
    private int isbn;
    private String title;
    private int numberOfCopies;
    private int copiesLeft;
    private int editionNumber;
    private String authorSurnames;

    Book(int isbn, String title, int numberOfCopies, int copiesLeft, int editionNumber, String authorSurname) {
        this.isbn = isbn;
        this.title = title;
        this.numberOfCopies = numberOfCopies;
        this.copiesLeft = copiesLeft;
        this.editionNumber = editionNumber;
        this.authorSurnames = authorSurname;
    }

    public int getIsbn() { return isbn; }

    public String getTitle() { return title; }

    public int getNumberOfCopies() { return numberOfCopies; }

    public int getCopiesLeft() { return copiesLeft; }

    public int getEditionNumber() { return editionNumber; }

    public String getAuthorSurnames() { return authorSurnames; }

    public void addAuthorSurname(String surname) {
        this.authorSurnames += ", " + surname;
    }
}