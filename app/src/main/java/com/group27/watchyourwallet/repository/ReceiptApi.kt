import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path

interface ReceiptApi {

    // GET all receipts for a user
    @GET("/receipts/{userId}")
    fun getReceipts(@Path("userId") userId: String): Call<List<Receipt>>

    // POST a new receipt
    @POST("/receipts")
    fun saveReceipt(@Body receipt: Receipt): Call<Void>
}