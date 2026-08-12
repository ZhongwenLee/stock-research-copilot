package com.stockresearch.copilot.parser;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DocxDocumentParser implements DocumentParser {

	private final TextCleaner textCleaner;

	@Override
	public boolean supports(String fileExt) {
		return "docx".equalsIgnoreCase(fileExt);
	}

	@Override
	public ParsedDocument parse(Path filePath) {
		try (InputStream in = Files.newInputStream(filePath); XWPFDocument document = new XWPFDocument(in)) {
			XWPFStyles styles = document.getStyles();
			List<ParsedSection> sections = new ArrayList<>();
			String currentTitle = "正文";
			StringBuilder buffer = new StringBuilder();
			String docTitle = null;

			for (IBodyElement element : document.getBodyElements()) {
				if (!(element instanceof XWPFParagraph paragraph)) {
					continue;
				}
				String text = textCleaner.clean(paragraph.getText());
				if (text.isBlank()) {
					continue;
				}
				if (isHeading(paragraph, styles)) {
					if (buffer.length() > 0) {
						sections.add(ParsedSection.builder()
								.titlePath(currentTitle)
								.section(currentTitle)
								.content(buffer.toString())
								.build());
						buffer.setLength(0);
					}
					currentTitle = text;
					if (docTitle == null) {
						docTitle = text;
					}
				}
				else {
					if (buffer.length() > 0) {
						buffer.append('\n');
					}
					buffer.append(text);
				}
			}
			if (buffer.length() > 0) {
				sections.add(ParsedSection.builder()
						.titlePath(currentTitle)
						.section(currentTitle)
						.content(buffer.toString())
						.build());
			}
			if (sections.isEmpty()) {
				sections.add(ParsedSection.builder()
						.titlePath("正文")
						.section("正文")
						.content("")
						.build());
			}
			String title = docTitle == null ? filePath.getFileName().toString() : docTitle;
			return ParsedDocument.builder().title(title).sections(sections).build();
		}
		catch (Exception ex) {
			throw new IllegalStateException("failed to parse docx: " + ex.getMessage(), ex);
		}
	}

	private boolean isHeading(XWPFParagraph paragraph, XWPFStyles styles) {
		String styleId = paragraph.getStyle();
		if (styleId == null) {
			return false;
		}
		String lower = styleId.toLowerCase(Locale.ROOT);
		if (lower.startsWith("heading") || lower.startsWith("标题")) {
			return true;
		}
		if (styles != null) {
			XWPFStyle style = styles.getStyle(styleId);
			if (style != null && style.getName() != null) {
				String name = style.getName().toLowerCase(Locale.ROOT);
				return name.startsWith("heading") || name.startsWith("标题");
			}
		}
		return false;
	}
}
