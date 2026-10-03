package app.webbangdiem.examination;

import app.webbangdiem.submission.TranscriptSummary;
import java.util.List;

public record TranscriptPage(List<TranscriptSummary> items, int page, int size, long total) {}
