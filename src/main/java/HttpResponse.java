public record HttpResponse(String headers, byte[] body) {
    public static HttpResponse plainText(String fullResponse) {
        return new HttpResponse(fullResponse, null);
    }

    public static HttpResponse withBinary(String headers, byte[] body) {
        return new HttpResponse(headers, body);
    }
}