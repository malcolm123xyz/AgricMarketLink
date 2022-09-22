package mx.mobile.solution.nabia04_beta1.utilities

data class Response<out T>(val status: Status, val data: T?, val message: String?) {

    companion object {

        fun <T> success(data: T?): Response<T> {
            return Response(Status.SUCCESS, data, null)
        }

        fun <T> error(msg: String, data: T?): Response<T> {
            return Response(Status.ERROR, data, msg)
        }

        fun <T> loading(data: T?): Response<T> {
            return Response(Status.LOADING, data, null)
        }

        fun <T> emitEvent(event: String): Response<T> {
            return Response(Status.EVENT, null, event)
        }

    }

}