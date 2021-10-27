package testserver;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GameTest {
	private char[][] board; 
	private static final int NUM_ROWS = 6;
	private static final int NUM_COLS = 9;
	private static final int WIN_NUM = 5;
	public boolean isWon = false;
	public boolean isOver = false;
	private boolean playerOneTurn = true;
	
	public GameTest(){
		this.board = new char[NUM_ROWS][NUM_COLS];
		for (int row = 0; row < NUM_ROWS; row++) {
			for(int col = 0; col < NUM_COLS; col++) {
				this.board[row][col] = ' ';
			}
		}
	}
	
	@Test
	void testToString() {
		// First check if String representation of empty board is correct
        Game game = new Game();
		StringBuilder res = new StringBuilder();
		for(int row = 0; row < NUM_ROWS; row++) {
			StringBuilder line = new StringBuilder();
			for(int col = 0; col < NUM_COLS; col++) {
				line.append("[ ]");
			}
			res.append(line);
			res.append(System.getProperty("line.separator"));
		}

        String expected =  res.toString();
		System.out.println(expected);
        assertEquals(expected, game.toString());
        
        // Next check if it handles printing the board after user makes turn (col 4 here)
        int turn = 3;
        game.takeTurn(turn);
		res = new StringBuilder();
		for(int row = 0; row < NUM_ROWS; row++) {
			StringBuilder line = new StringBuilder();
			for(int col = 0; col < NUM_COLS; col++) {
				if(col == turn && row == NUM_ROWS -1) {
					line.append("[x]");
				}else {
					line.append("[ ]");
				}
			}
			res.append(line);
			res.append(System.getProperty("line.separator"));
		}

			expected = res.toString();
			assertEquals(expected, game.toString());
		}
	
	@Test
	void testAddPiece() {
		// First check that the correct row is returned upon adding piece to column
		// if we drop a piece in column 1, and drop another into col. 1 then row 2 should be returned
		// Chose not to test what happens if addPiece is called on a full column
		// as any time the addPiece() method is called, the checkColFull() method is first called to see if its a valid move
		// and I wanted to minimise interdependent test cases as addPiece is dependent on checkColFull in the control flow
		Game game = new Game();
		int col = 0;
		int firstExpRow = game.addPiece(col);
		int bottomRow = NUM_ROWS -1;
		int secondBottomRow = NUM_ROWS -2;
		// row should equal 5 (bottom row index)
		assertEquals(firstExpRow, bottomRow);
		game.changePlayerOneTurn();
		int secondExpRow = game.addPiece(col);
		// row should equal 4 (next available row)
		assertEquals(secondExpRow, secondBottomRow);
		// Next check if first move piece is correctly placed on board
		char x = 'x';
		char o = 'o';
		assertEquals(x, game.getPieceAtIndex(firstExpRow, col));
		// Check if second move piece is correctly placed on board
		assertEquals(o, game.getPieceAtIndex(secondExpRow, col));
		
		
	}
	
	@Test
	void testCheckColFull() {
		// Check that this method returns false while column is not full and true when it is
		Game game = new Game();
		int col = 0;
		int counter = 0;
		while(counter < NUM_ROWS) {
			assertFalse(game.checkColFull(col));
			game.addPiece(col);
			game.changePlayerOneTurn();
			counter++;
		}
		assertTrue(game.checkColFull(col));
		// Check another random column to ensure the same results
		col = 1;
		counter = 0;
		while(counter < NUM_ROWS) {
			assertFalse(game.checkColFull(col));
			game.addPiece(col);
			game.changePlayerOneTurn();
			counter++;
		}
		assertTrue(game.checkColFull(col));
		
		
	}
	
	@Test
	void testCheckPiece() {
		// Ensure this method returns 'x' when player 1's turn and 'o' when player 2's turn
		Game game = new Game();
		// player one's turn initially, should return 'x'
		assertEquals('x', game.checkPiece());
		//change turn and ensure it returns 'o'
		game.changePlayerOneTurn();
		assertEquals('o', game.checkPiece());
		// change turn again and ensure it returns 'x'
		game.changePlayerOneTurn();
		assertEquals('x', game.checkPiece());
		
	}
	
	@Test
	// This checks for 5 in a row on the horizontal plane of last moved piece
	void testCheckHorizontal() {
		// continuously populate bottom row with pieces and check for 5 in a row
		// First check if it can spot 5 in a row by just placing 5 'x's in a row on bottom row
		// i.e. not changing playerTurn after each move
		// we don't need to simulate player turn changing after every addPiece() because in checkHorizontal()
		// it first checks what piece its looking for in the row provided by calling the checkPiece() method which returns
		// 'x' or 'o' depending on who's turn it is
		Game game = new Game();
		int col = 0;
		while(col < WIN_NUM - 1) {
			int row = game.addPiece(col);
			col++;
			assertFalse(game.checkHorizontal(row));
		}
		int row = game.addPiece(col);
		assertTrue(game.checkHorizontal(row));
		
		// now put another horizontal row on top of that
		col = 0;
		while(col < WIN_NUM - 1) {
			row = game.addPiece(col);
			col++;
			assertFalse(game.checkHorizontal(row));
		}
		row = game.addPiece(col);
		assertTrue(game.checkHorizontal(row));	
	}
	
	@Test
	// This checks for 5 in a row on the vertical plane of last moved piece
	void testCheckVertical() {
		Game game = new Game();
		// test for basic vertical five in a row on first col
		int col = 0;
		int counter = 0;
		// make sure it returns false until there is 5 in a row
		while(counter < WIN_NUM - 1) {
			game.addPiece(col);
			assertFalse(game.checkVertical(col));
			counter++;
		}
		// return true now that there is 5 in a row vertically 
		game.addPiece(col);
		assertTrue(game.checkVertical(col));
		
		// Test for 5 in a row on top of other piece
		col = 4;
		counter = 0;
		// put one 'x' down on column 4
		game.addPiece(col);
		assertFalse(game.checkVertical(col));
		// put 5 'o's on top of it and check for win
		game.changePlayerOneTurn();
		// make sure it returns false until there is 5 in a row
		while(counter < WIN_NUM - 1) {
			game.addPiece(col);
			assertFalse(game.checkVertical(col));
			counter++;
		}
		// return true now that there is 5 in a row vertically 
		game.addPiece(col);
		assertTrue(game.checkVertical(col));
		
	}
	
	// Helper method for populating columns with pieces
	public void populateColumn(int col, int numX, Game game) {
		int counter = 0;
		while(counter < numX) {
			game.addPiece(col);
			counter++;
		}
	}
	
	
	@Test
	void testCheckLeftDiagonal() {
		//  [ ][ ][ ][ ][ ][ ][ ][ ][ ]
		//	[ ][ ][ ][ ][o][ ][ ][ ][ ]
		//	[ ][ ][ ][o][x][ ][ ][ ][ ]
		//	[ ][ ][o][x][x][ ][ ][ ][ ]
		//	[ ][o][x][x][x][ ][ ][ ][ ]
		//	[o][x][x][x][x][ ][ ][ ][ ]
		// testing for this case
		// with various last move indices 
		Game game = new Game();
		// first populate board with Xs (brute forcing this, I'm sure there's probably a more elegant way of doing this)
		
		// add 4 x's to 5th column (4 indexed)
		int counter = 0;
		int col = 4;
		int numX = 4;
		populateColumn(col, numX, game);
		// add 3 x's to 4th column (3 indexed)
		counter = 0;
		col = 3;
		numX = 3;
		populateColumn(col, numX, game);
		// add 2 x's to 3rd column (2 indexed)
		counter = 0;
		col = 2;
		numX = 2;
		populateColumn(col, numX, game);
		// add 1 x to 2nd column (1 indexed)
		counter = 0;
		col = 1;
		numX = 1;
		populateColumn(col, numX, game);
		// add 1 o to 1st column (0 indexed)
		game.changePlayerOneTurn();
		col = 0;
		counter = 0;
		while(counter < WIN_NUM) {
			game.addPiece(col);
			counter++;
			col++;
		}
		// Board is now populated as above
		// check by printing that it is same as commented above
		System.out.println(game.toString());
		// Now check it confirms a left to right diagonal using different last moves
		// e.g. test all 'o's individually to see if they are part of a 5 in a row
		col = 0;
		int row = NUM_ROWS-1;
		int endIndex = 5;
		while(col < endIndex) {
			assertTrue(game.checkLeftDiagonal(row, col));
			col++;
			row--;
		}
		// Check squares which are not part of a 5 in a row to make sure there are no false positives
		// e.g. the other corner cells
		//top left (0,0)
		row = 0;
		col = 0;
		assertFalse(game.checkLeftDiagonal(row, col));
		// top right (0,8)
		row = 0;
		col = 8;
		assertFalse(game.checkLeftDiagonal(row, col));
		// bottom right (5,8)
		row = 5;
		col = 8;
		assertFalse(game.checkLeftDiagonal(row, col));
		
		// Check an arbitrary x square 
		row = 3;
		col = 3;
		assertFalse(game.checkLeftDiagonal(row, col));
	}
	
	// Aware of the repeated code here
	@Test
	void testCheckRightDiagonal() {
		//  [ ][ ][ ][ ][ ][ ][ ][ ][ ]
		//	[ ][ ][ ][ ][o][ ][ ][ ][ ]
		//	[ ][ ][ ][ ][x][o][ ][ ][ ]
		//	[ ][ ][ ][ ][x][x][o][ ][ ]
		//	[ ][ ][ ][ ][x][x][x][o][ ]
		//	[ ][ ][ ][ ][x][x][x][x][o]
		// testing for this case
		// with various last move indices 
		Game game = new Game();
		// first populate board with Xs (brute forcing this, I'm sure there's probably a more elegant way of doing this)
		// add 4 x's to 5th column (4 indexed)
		int counter = 0;
		int col = 4;
		int numX = 4;
		populateColumn(col, numX, game);
		// add 3 x's to 6th column (5 indexed)
		counter = 0;
		col = 5;
		numX = 3;
		populateColumn(col, numX, game);
		// add 2 x's to 7th column (6 indexed)
		counter = 0;
		col = 6;
		numX = 2;
		populateColumn(col, numX, game);
		// add 1 x to 8th column (7 indexed)
		counter = 0;
		col = 7;
		numX = 1;
		populateColumn(col, numX, game);
		// add 1 o to 9th column (8 indexed)
		game.changePlayerOneTurn();
		col = 4;
		counter = 0;
		while(counter < WIN_NUM) {
			game.addPiece(col);
			counter++;
			col++;
		}
		// Board is now populated as above
		// check by printing that it is same as commented above
		System.out.println(game.toString());
		// Now check it confirms a right to left diagonal using different last moves
		// e.g. test all 'o's individually to see if they are part of a 5 in a row
		col = 4;
		int row = 1;
		int endIndex = NUM_COLS;
		while(col < endIndex) {
			assertTrue(game.checkRightDiagonal(row, col));
			col++;
			row++;
		}
		// Check squares which are not part of a 5 in a row to make sure there are no false positives
		// e.g. the other corner cells
		//top left (0,0)
		row = 0;
		col = 0;
		assertFalse(game.checkRightDiagonal(row, col));
		// top right (0,8)
		row = 0;
		col = 8;
		assertFalse(game.checkRightDiagonal(row, col));
		// bottom left (5,0)
		row = 5;
		col = 0;
		assertFalse(game.checkRightDiagonal(row, col));
		
		// Check an arbitrary x square 
		row = 3;
		col = 3;
		assertFalse(game.checkRightDiagonal(row, col));
	}
	
	@Test
	void testCheckBoardFull() {
		// Demonstrate it detects when the board is full
		int col = 0;
		Game game = new Game();
		// First show it can handle empty board
		assertFalse(game.checkBoardFull());
		while(col < NUM_COLS) {
			populateColumn(col, NUM_ROWS, game);
			game.changePlayerOneTurn();
			col++;
		}
		// check board is visually full
		System.out.println(game.toString());
		// check if it can handle the board being full
        assertTrue(game.checkBoardFull());
      
	
	}
	
	
	
        

}
