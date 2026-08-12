package com.stockresearch.copilot.parser;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class HtmlDocumentParser implements DocumentParser {

	private final TextCleaner textCleaner;

	@Override
	public boolean supports(String fileExt) {
		String ext = fileExt == null ? "" : fileExt.toLowerCase(Locale.ROOT);
		return "html".equals(ext) || "htm".equals(ext);
	}

	@Override
	public ParsedDocument parse(Path filePath) {
		try {
			String raw = Files.readString(filePath, StandardCharsets.UTF_8);
			Document doc = Jsoup.parse(raw);
			doc.select("script,style,noscript").remove();
			String title = textCleaner.clean(doc.title());
			if (title.isBlank()) {
				title = filePath.getFileName().toString();
			}

			List<ParsedSection> sections = new ArrayList<>();
			Elements headings = doc.select("h1,h2,h3,h4,h5,h6");
			if (headings.isEmpty()) {
				String body = textCleaner.clean(doc.body() == null ? doc.text() : doc.body().text());
				sections.add(ParsedSection.builder()
						.titlePath(title)
						.section(title)
						.content(body)
						.build());
			}
			else {
				for (Element heading : headings) {
					String sectionTitle = textCleaner.clean(heading.text());
					StringBuilder content = new StringBuilder();
					Element cursor = heading.nextElementSibling();
					while (cursor != null && !cursor.tagName().matches("h[1-6]")) {
						String piece = textCleaner.clean(cursor.text());
						if (!piece.isBlank()) {
							if (content.length() > 0) {
								content.append('\n');
							}
							content.append(piece);
						}
						cursor = cursor.nextElementSibling();
					}
					if (content.length() == 0) {
						continue;
					}
					sections.add(ParsedSection.builder()
							.titlePath(sectionTitle)
							.section(sectionTitle)
							.content(content.toString())
							.build());
				}
			}

			if (sections.isEmpty()) {
				sections.add(ParsedSection.builder()
						.titlePath(title)
						.section(title)
						.content(textCleaner.clean(doc.text()))
						.build());
			}
			return ParsedDocument.builder().title(title).sections(sections).build();
		}
		catch (Exception ex) {
			throw new IllegalStateException("failed to parse html: " + ex.getMessage(), ex);
		}
	}
}
