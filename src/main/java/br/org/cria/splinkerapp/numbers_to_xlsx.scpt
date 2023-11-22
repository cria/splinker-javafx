
-- Define the input and output file paths
set inputFilePath to "/users/brunobemfica/downloads/datasources/planilha_fvd (autosaved) 2.numbers"
set outputFilePath to "/users/brunobemfica/downloads/datasources/planilha_fvd (autosaved) 2.xlsx"


tell application "Numbers"
	activate
	try
		if not (exists inputFilePath) then error number -128
		
		-- DERIVE NAME AND FILE PATH FOR NEW EXPORT FILE
		set documentName to the name of the front document
		if documentName ends with ".numbers" then ¬
			set documentName to text 1 thru -9 of documentName
		
		tell application "Finder"
			set newExportFileName to documentName & "." & exportFileExtension
			set incrementIndex to 1
			repeat until not (exists document file newExportFileName of defaultDestinationFolder)
				set newExp6ortFileName to ¬
					documentName & "-" & (incrementIndex as string) & "." & exportFileExtension
				set incrementIndex to incrementIndex + 1
			end repeat
		end tell
		set the targetFileHFSPath to (defaultDestinationFolder as string) & newExportFileName
		
		-- EXPORT THE DOCUMENT
		with timeout of 1200 seconds
			export front document to file targetFileHFSPath as Microsoft Excel
		end timeout
		
	on error errorMessage number errorNumber
		display alert "EXPORT PROBLEM" message errorMessage
		error number -128
	end try
end tell