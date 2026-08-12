package com.stockresearch.copilot.parser;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PdfDocumentParser implements DocumentParser {

	private static final Pattern HEADING = Pattern.compile(
			"^(第[一二三四五六七八九十百千0-9]+[章节部分]|[一二三四五六七八九十]+[、.]|\\d+(\\.\\d+)*\\s+).+");

	private final TextCleaner textCleaner;
	private final TxtDocumentParser txtDocumentParser;

	@Override
	public boolean supports(String fileExt) {
		return "pdf".equalsIgnoreCase(fileExt);
	}

	@Override
	public ParsedDocument parse(Path filePath) {
		try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
			PDFTextStripper stripper = new PDFTextStripper();
			List<ParsedSection> sections = new ArrayList<>();
			int pages = document.getNumberOfPages();
			for (int page = 1; page <= pages; page++) {
				stripper.setStartPage(page);
				stripper.setEndPage(page);
				String pageText = textCleaner.clean(stripper.getText(document));
				if (pageText.isBlank()) {
					continue;
				}
				List<ParsedSection> pageSections = txtDocumentParser.splitByHeading(pageText);
				for (ParsedSection section : pageSections) {
					section.setPageNo(page);
					if (section.getTitlePath() == null || section.getTitlePath().isBlank()) {
						section.setTitlePath("第" + page + "页");
						section.setSection("第" + page + "页");
					}
					sections.add(section);
				}
			}
			String title = filePath.getFileName().toString();
			if (!sections.isEmpty() && sections.get(0).getSection() != null) {
				Matcher matcher = HEADING.matcher(sections.get(0).getSection());
				if (matcher.find()) {
					title = sections.get(0).getSection();
				}
			}
			if (sections.isEmpty()) {
				sections.add(ParsedSection.builder()
						.titlePath("正文")
						.section("正文")
						.content("")
						.pageNo(1)
						.build());
			}
			return ParsedDocument.builder().title(title).sections(sections).build();
		}
		catch (Exception ex) {
			throw new IllegalStateException("failed to parse pdf: " + ex.getMessage(), ex);
		}
	}
}
