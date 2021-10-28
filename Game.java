package testserver;


public class Game {
	private char[][] board; // board represented by 2d array
	private static final int NUM_ROWS = 6;
	private static final int NUM_COLS = 9;
	private static final int WIN_NUM = 5;
	private boolean isWon = false;
	private boolean isOver = false;
	private boolean playerOneTurn = true;
	
	// populate board upon initialisation
	public Game() {
		this.board = new char[NUM_ROWS][NUM_COLS];
		for (int row = 0; row < NUM_ROWS; row++) {
			for(int col = 0; col < NUM_COLS; col++) {
				this.board[row][col] = ' ';
			}
		}
	}
	// These getter and setters are needed so that the application server can access the Game object's variables
	public boolean getPlayerOneTurn() {
		return this.playerOneTurn;
	}
	public boolean getIsOver() {
		return this.isOver;
	}
	public boolean getIsWon() {
		return this.isWon;
	}
	public void setIsWon() {
		this.isWon = true;
	}
	public void setIsOver() {
		this.isOver = true;
	}
	public void changePlayerOneTurn() {
		this.playerOneTurn = !this.playerOneTurn;
	}
	
	public char getPieceAtIndex(int row, int col) {
		return this.board[row][col]; 
	}
	// Prints out string representation of board
	public String toString() {
		StringBuilder res = new StringBuilder();
		for(int row = 0; row < NUM_ROWS; row++) {
			StringBuilder line = new StringBuilder();
			for(int col = 0; col < NUM_COLS; col++) {
				line.append("[" + this.board[row][col] + "]");
			}
			res.append(line);
			res.append(System.getProperty("line.separator"));
		}
		return res.toString();
	}
	
	// Drops piece down column
	// returns row it lands on so that checkWin() knows exact location of last move
	public int addPiece(int col) {
		int row = NUM_ROWS - 1;
		// get next available row
		while(this.board[row][col] != ' '){
				row--;
		}
		// place piece here
		char piece = this.checkPiece();
		this.board[row][col] = piece;
		return row;
	}
	
	// Checks if column is full
	public boolean checkColFull(int col) {
		if(this.board[0][col] == ' ') {
			return false;
		}else {
			return true;
		}
	}
	
	
	// returns piece (x or o) depending on who's turn it is 
    public char checkPiece() {
    	char piece;
		if (playerOneTurn == true) {
			piece = 'x';
		}else {
			piece = 'o';
		}
		return piece;
    }
    
    // method to move piece
    // method not unit tested as it just calls other methods and performs basic operations based on their results
    public void takeTurn(int col) {
    	int row = this.addPiece(col);
    	if(this.checkWin(row, col)){
    		this.isWon = true;
   			this.isOver = true;
   		}
   		else if(this.checkBoardFull()) {
   			this.isOver = true;
    	}
    	this.playerOneTurn = !this.playerOneTurn;
    }
    
    // Checks horizontal plain of dropped piece
	public boolean checkHorizontal(int row) {
		char piece = this.checkPiece();
		int longestStreak = 0;
		for(int i = 0; i < NUM_COLS; i++) {
			if(this.board[row][i] == piece) {
				longestStreak++;
				if(longestStreak >= WIN_NUM) {
					return true;
				}
			}else {
				longestStreak = 0;
			}
		}
		return false;
	}
	
	// Checks if 5 in a row on vertical plain
	public boolean checkVertical(int col) {
		char piece = this.checkPiece();
		int longestStreak = 0;
		for(int i = 0; i < NUM_ROWS; i++) {
			if(this.board[i][col] == piece) {
				longestStreak++;
				if(longestStreak >= WIN_NUM) {
					return true;
				}
			}else {
				longestStreak = 0;
			}
		}
		return false;
	}
    // Checks bottom left to top right diagonal for 5 in a row
	// Its a little inelegant as it first gets the starting row/col
	// by iterating downwards then scans upward diagonally to the right, passing through the last dropped piece
	// squares could be checked while moving down to starting position
	// but this was used in the interest of time, readability, and because the board will never be big enough for this to be a problem
	public boolean checkLeftDiagonal(int row, int col) {
		char piece = this.checkPiece();
		int longestStreak = 0;
		int counter = 0;
		int startRow = row;
		int startCol = col;
		while(counter < NUM_ROWS && startCol > 0 && startRow < NUM_ROWS - 1) {
			startRow ++;
			startCol--;
			counter++;
		}
		
        counter = 0;
		while(counter < NUM_ROWS && startCol <= NUM_COLS - 1 && startRow >= 0) {
			if(this.board[startRow][startCol] == piece) {
				longestStreak++;
				if(longestStreak >= WIN_NUM) {
					return true;
				}
			}else {
				longestStreak = 0;
			}
			counter++;
			startRow--;
			startCol++;
		}
		return false;
	}
	// Does the same as checkLeftDiagonal but starts from bottom right and scans upward to the left, passing through the last piece dropped
	public boolean checkRightDiagonal(int row, int col) {
		char piece = this.checkPiece();
		int longestStreak = 0;
		int counter = 0;
		int startRow = row;
		int startCol = col;
		while(counter < NUM_ROWS && startCol < NUM_COLS - 1 && startRow < NUM_ROWS - 1) {
			startRow ++;
			startCol++;
			counter++;
		}
		
        counter = 0;
		while(counter < NUM_ROWS && startCol >= 0 && startRow >= 0) {
			if(this.board[startRow][startCol] == piece) {
				longestStreak++;
				if(longestStreak >= WIN_NUM) {
					return true;
				}
			}else {
				longestStreak = 0;
			}
			counter++;
			startRow--;
			startCol--;
		}
		return false;
	}
	

	
	
	public boolean checkDiagonal(int row, int col) {
		return this.checkLeftDiagonal(row, col) || this.checkRightDiagonal(row,  col);
	}
	
	
	//checks win
	public boolean checkWin(int row, int col) {
		return this.checkHorizontal(row) || this.checkVertical(col) || this.checkDiagonal(row, col);
	}
	
	// checks if board is full
	public boolean checkBoardFull() {
		for(int i = 0; i < NUM_COLS; i++) {
			if(this.board[0][i] == ' ') {
				return false;
			}
		}
		return true;
	}
	
	public static void main(String[] args){

	}

}
