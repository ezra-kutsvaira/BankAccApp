package bankAccApp;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.Scanner;

public class BankAccApp {
	public static void main(String...args) throws InsufficientFundsException {
		AccountService accountService = new AccountService(new FileAccountRepositoryImpl());
        TransactionService transactionService = new TransactionService(new FileTransactionRepositoryImpl());
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("__________________________________");
            System.out.println(" Welcome to ABC Bank App ");
            System.out.println("__________________________________");

            // main menu
            System.out.println("What do you want to do today?");
            System.out.println("1. Register an account.");
            System.out.println("2. Transact.");

            int mainMenuResponse = scanner.nextInt();

            if (mainMenuResponse == 1) {
                // load create account menu.
                accountService.createAccount();
            } else {
                // load transact menu
                transact(scanner, accountService, transactionService);
            }
        } catch (AccountAlreadyExistsException ex) {
            // If account already exists ask user to deposit/withdraw money instead.
            System.out.println("Do you want to transact instead? (Y/N): ");
            String response = scanner.next();

            if (response.equalsIgnoreCase("Y")) {
                transact(scanner, accountService, transactionService);
            } else {
                System.out.println("Session closed. Thank you for using our services!!");
            }
        }
        scanner.close();
    }

    public static void transact(Scanner scanner, AccountService accountService, TransactionService transactionService) throws InsufficientFundsException {
        // Menu of options
        List<String> options = Arrays.asList("0. Check Balance", "1. Deposit", "2. Withdraw", "3. Transfer");
        System.out.println("What transaction do you want to perform? ");

        options.forEach(System.out::println);

        int userSelection = scanner.nextInt();
        switch (userSelection) {
            case 0:
                //Check account balance
                String getBalanceAccountNumber = getAccountInput(scanner, accountService);
                System.out.println("Your account balance is: " + transactionService.getBalance(getBalanceAccountNumber));
                break;
            case 1:
                //Deposit money
                String accountNumber = getAccountInput(scanner, accountService);
                System.out.println("Enter the amount you want to deposit");
                double depositAmount = scanner.nextDouble();

                // Deposit
                transactionService.deposit(accountNumber, depositAmount);
                System.out.println("Your account balance is: " + transactionService.getBalance(accountNumber));
                break;

            case 2:
                //Withdraw money
                System.out.println("Enter the amount you want to withdraw");
                double withdrawalAmount = scanner.nextDouble();
                transactionService.withdraw(getAccountInput(scanner, accountService), withdrawalAmount);
                break;

            case 3:
                //Transfer money
                System.out.println("Enter the account number you want to transfer to: ");
                String otherAccountNumber = scanner.next();
                System.out.println("Enter the amount you want to transfer: ");
                double transferAmount = scanner.nextDouble();
                transactionService.transfer(getAccountInput(scanner, accountService), otherAccountNumber, transferAmount);
                break;


            default:
                System.out.println("Invalid option");
                break;
        }
        scanner.close();
    }

    private static String getAccountInput(Scanner scanner, AccountService accountService) {
        //Check account balance
        System.out.println("Enter your account number: ");
        String account = scanner.next();

        // check if account exists
        Optional<Account> optionalAccount = accountService.getAccount(account);
        if (optionalAccount.isEmpty()) {
            System.out.println("Invalid account number, session closed.");
            scanner.close();
            return "";
        }
        return account;
    }
}

class TransactionService {
    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    // deposit
    public void deposit(String accountNumber, double amount) {
        repository.save(new Transaction(accountNumber, amount));
    }

    // withdraw
    public void withdraw(String accountNumber, double amount) throws InsufficientFundsException{
    	double balance = getBalance(accountNumber);
    	if(balance < amount) {
    		throw new InsufficientFundsException(amount - balance);
    	}
    	repository.save(new Transaction(accountNumber, -amount));
    }

    // transfer
    public void transfer(String fromAccountNumber, String toAccountNumber, double amount) throws InsufficientFundsException{
    	 withdraw(fromAccountNumber, amount);
    	    repository.save(new Transaction(toAccountNumber, amount));
    }
     

    // Get all transactions by account
    private List<Transaction> getTransactions(String accountNumber) {
        return repository.getTransactions(accountNumber);
    }

    // Get account balance
    public double getBalance(String accountNumber) {
        return repository.getTransactions(accountNumber).stream()
                .mapToDouble(t -> t.getAmount())
                .sum();
    }
}

class AccountService {
    private final Scanner scanner;
    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.scanner = new Scanner(System.in);
        this.repository = repository;
    }

    public void createAccount() throws AccountAlreadyExistsException {
        System.out.println("Enter your name");
        String accHolderName = scanner.nextLine();

        System.out.println("Enter ID Number (xx-xxxxxxxnxx) ");
        String idNumber = scanner.nextLine();

        Set<Account> existingAccounts = repository.getAll();

        // check if id number exists in database
        if (AccountUtil.idNumberExists(idNumber, existingAccounts)) {
            System.out.println("ID Number already exists");
            throw new AccountAlreadyExistsException("ID Number already exists");
        }

        System.out.println("Enter D.O.B in YYYY-MM-DD format ");
        String dob = scanner.nextLine();
        System.out.println("DOB of user is " + dob);

        //Calculate users age
        Period age = Period.between(LocalDate.parse(dob), LocalDate.now());
        int years = age.getYears();

        if (years < 18) {
            System.out.println("Sorry you are not old enough to register");
            return;
        }

        Account account = new Account(accHolderName, idNumber, dob, generateAccountNumber());

        // Save account to database;
        repository.save(account);
        System.out.print("Your New Account Number " + account.getAccountNumber());
    }

    public Optional<Account> getAccount(String accountNumber) {
        return repository.getByAccountNumber(accountNumber);
    }

    public String generateAccountNumber() {
        int minRange = 1;
        int maxRange = 10;
        int count = 8;

        Random random = new Random();
        List<Integer> generatedNumbers = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int randomNumber;
            do {
                randomNumber = random.nextInt(maxRange - minRange + 1) + minRange;
            } while (generatedNumbers.contains(randomNumber));
            generatedNumbers.add(randomNumber);
        }

        StringBuilder accountNumberBuilder = new StringBuilder();
        for (Integer number : generatedNumbers) {
            accountNumberBuilder.append(number);
        }

        return accountNumberBuilder.toString();
    }
}

class Account {
    //Account Instance variables
    private final String accountHolderName;
    private final String idNumber;
    private final String dateOfBirth;
    private final String accountNumber;

    //creation of the class constructor
    public Account(String accountHolderName, String idNumber, String dateOfBirth, String accountNumber) {
        this.accountHolderName = accountHolderName;
        this.idNumber = idNumber;
        this.dateOfBirth = dateOfBirth;
        this.accountNumber = accountNumber;
    }

    @Override
    public String toString() {
        return this.accountHolderName + "::" + this.idNumber + "::" + this.dateOfBirth + "::" + this.accountNumber + "\n";
    }

    // Getters
    public String getAccountHolderName() {
        return this.accountHolderName;
    }

    public String getIdNumber() {
        return this.idNumber;
    }

    public String getAccountNumber() {
        return this.accountNumber;
    }
}

class AccountUtil {
    static boolean idNumberExists(String idNumber, Set<Account> accounts) {
        return accounts.stream().anyMatch(account -> account.getIdNumber().equals(idNumber));
    }

    static boolean nameExists(String name, Set<Account> accounts) {
        return accounts.stream().anyMatch(account -> account.getAccountHolderName().equals(name));
    }
}

class FileAccountRepositoryImpl implements AccountRepository {
    private static final String FILE_PATH = "C:\\Users\\HP\\Documents\\text-database.txt";

    public void save(Account account) {
        try {
            // File path
            Path path = Paths.get(FILE_PATH);

            // Check if file exists, create if not.
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                Files.createFile(path);
            }

            // Append the account record to existing records in the file.
            Files.write(path, account.toString().getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ex) {
            // Catch exception and fail gracefully.
            ex.printStackTrace();
        }
    }

    public Set<Account> getAll() {
        try {
            return Files.readAllLines(Paths.get(FILE_PATH))
                    .stream()
                    .map(this::toAccount)
                    .collect(Collectors.toSet());
        } catch (IOException ex) {
            // Catch exception and fail gracefully.
        }
        return new HashSet<>();
    }

    @Override
    public Optional<Account> getByAccountNumber(java.lang.String accountNumber) {
        try {
            return Files.readAllLines(Paths.get(FILE_PATH)).stream()
                    .map(this::toAccount)
                    .filter(acc -> acc.getAccountNumber().equals(accountNumber))
                    .findFirst();
        } catch (IOException ex) {
            // Catch exception and fail gracefully.
        }
        return Optional.empty();
    }

    private Account toAccount(String record) {
        String[] arr = record.split("::");
        return new Account(arr[0], arr[1], arr[2], arr[3]);
    }

    public void saveAll(Set<Account> accounts) {
        try {
            // File path
            Path path = Paths.get(FILE_PATH);

            // Write all account records to the file
            StringBuilder data = new StringBuilder();
            for (Account account : accounts) {
                data.append(account.toString());
            }
            Files.write(path, data.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            // Catch exception and fail gracefully.
            ex.printStackTrace();
        }
    }
}

class Transaction {
    private String accountNumber;
    private double amount; // try using BigDecimal

    public Transaction(String accountNumber, double amount) {
        this.accountNumber = accountNumber;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return this.accountNumber + "::" + this.amount + "\n";
    }

    public String getAccountNumber() {
        return this.accountNumber;
    }

    public double getAmount() {
        return this.amount;
    }
}

class FileTransactionRepositoryImpl implements TransactionRepository {
    private static final String FILE_PATH =  "C:\\Users\\HP\\Documents\\transcations.txt";

    public void save(Transaction transaction) {
        try {
            // File path
            Path path = Paths.get(FILE_PATH);
            // Check if file exists, create if not.
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                Files.createFile(path);
            }
            // Append the transaction record to existing records in the file.
            Files.write(path, transaction.toString().getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ex) {
            // Catch exception and fail gracefully.
            ex.printStackTrace();
        }
    }

    public List<Transaction> getTransactions(String account) {
        try {
            // File path
            Path path = Paths.get(FILE_PATH);

            // Check if file exists, create if not.
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                Files.createFile(path);
            }
            return Files.readAllLines(Paths.get(FILE_PATH)).stream()
                    .map(this::toTransaction)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            // Catch exception and fail gracefully.
            ex.printStackTrace();
        }
        return new ArrayList<>();
    }

    private Transaction toTransaction(String record) {
        String[] arr = record.split("::");
        return new Transaction(arr[0], Double.parseDouble(arr[1]));
    }
}

interface AccountRepository {
    public void save(Account account);

    public Set<Account> getAll();

    public Optional<Account> getByAccountNumber(String accountNumber);
}

interface TransactionRepository {
    public void save(Transaction transaction);

    public List<Transaction> getTransactions(String accountNumber);
}

// Exception classes
class AccountAlreadyExistsException extends Exception {
    public AccountAlreadyExistsException(String message) {
        super(message);
    }
	}
class InsufficientFundsException extends Exception {

    private final double amount;

    public InsufficientFundsException(double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public String getMessage() {
        return "Insufficient funds. The amount you requested to withdraw is " + amount + " but your balance is less than that.";
    }
}


