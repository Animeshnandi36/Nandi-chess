package com.example.chess.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_games")
data class SavedChessGame(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fen: String,
    val pgn: String,
    val gameMode: String,
    val difficulty: String,
    val whiteName: String,
    val blackName: String,
    val result: String = "In Progress",
    val whiteTimeLeftMs: Long,
    val blackTimeLeftMs: Long,
    val isCompleted: Boolean = false
)

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_games ORDER BY timestamp DESC")
    fun getAllGames(): Flow<List<SavedChessGame>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: SavedChessGame): Long

    @Query("DELETE FROM saved_games WHERE id = :id")
    suspend fun deleteGameById(id: Int)

    @Query("SELECT * FROM saved_games WHERE id = :id LIMIT 1")
    suspend fun getGameById(id: Int): SavedChessGame?
}

@Database(entities = [SavedChessGame::class], version = 1, exportSchema = false)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun savedGameDao(): SavedGameDao
}

class ChessGameRepository(private val dao: SavedGameDao) {
    val allGames: Flow<List<SavedChessGame>> = dao.getAllGames()

    suspend fun saveGame(game: SavedChessGame): Long {
        return dao.insertGame(game)
    }

    suspend fun deleteGame(id: Int) {
        dao.deleteGameById(id)
    }

    suspend fun getGameById(id: Int): SavedChessGame? {
        return dao.getGameById(id)
    }
}
