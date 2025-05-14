enum ContentEncoding {
    NONE, GZIP;

    public static ContentEncoding fromHeader(String headerValue) {
        if (headerValue != null && headerValue.toLowerCase().contains("gzip")) {
            return GZIP;
        }
        return NONE;
    }
}