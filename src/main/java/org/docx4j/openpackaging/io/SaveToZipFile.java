/*
 *  Copyright 2007-2008, Plutext Pty Ltd.
 *   
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

 */

package org.docx4j.openpackaging.io;



import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.docx4j.openpackaging.URIHelper;
import org.docx4j.openpackaging.contenttype.ContentTypeManager;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.Package;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPart;
import org.docx4j.openpackaging.parts.relationships.RelationshipsPart;
import org.docx4j.relationships.Relationships;
import org.docx4j.relationships.Relationship;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;


/**
 * Save a Package object to a Zip file or output stream
 * @author jharrop
 *
 */
public class SaveToZipFile {
	
	private static Logger log = Logger.getLogger(SaveToZipFile.class);				
	
	public SaveToZipFile(Package p) {
		
		this.p = p;
		
	}
		
	// The package to save
	public Package p;
	

	/* Save a Package as a Zip file in the file system */
	public boolean save(String filepath) throws Docx4JException  {
		
		log.info("Saving to" +  filepath );		
		try {
			return save(new FileOutputStream(filepath));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	/* Save a Package as a Zip file in the file system */
	public boolean save(java.io.File docxFile) throws Docx4JException  {
		
		log.info("Saving to" +  docxFile );		
		try {
			return save(new FileOutputStream(docxFile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	/* Save a Package as a Zip file in the outputstream provided */
	public boolean save(OutputStream realOS) throws Docx4JException  {		
		
		 try {

			ZipOutputStream out = new ZipOutputStream(realOS);
			
			
			// 3. Get [Content_Types].xml
			ContentTypeManager ctm = p.getContentTypeManager();
			deprecatedSaveRawXmlPart(out, "[Content_Types].xml", ctm.getDocument() );
	        
			// 4. Start with _rels/.rels

//			<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
//			  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
//			  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
//			  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
//			</Relationships>		
			
			String partName = "_rels/.rels";
			RelationshipsPart rp = p.getRelationshipsPart();
			//deprecatedSaveRawXmlPart(out, partName, rp.getDocument() );
			// 2008 06 12 - try this neater method
			saveRawXmlPart(out, rp, partName );
			
			
			// 5. Now recursively 
//			addPartsFromRelationships(out, "", rp );
			addPartsFromRelationships(out, rp );
	    
			
	        // Complete the ZIP file
			// Don't forget to do this or everything will appear
			// to work, but when you open the zip file you'll get an error
			// "End-of-central-directory signature not found."
	        out.close();
	        realOS.close();
	    } catch (Exception e) {
			e.printStackTrace() ;
			if (e instanceof Docx4JException) {
				throw (Docx4JException)e;
			} else {
				throw new Docx4JException("Failed to save package", e);
			}
	    }

	    log.info("...Done!" );		

		 return true;
	}


	public void  saveRawXmlPart(ZipOutputStream out, Part part) throws Docx4JException {
		
		// This is a neater signature and should be used where possible!
		
		String partName = part.getPartName().getName().substring(1);

		saveRawXmlPart(out, part, partName);
	}
	
	public void  saveRawXmlPart(ZipOutputStream out, Part part, String zipEntryName) throws Docx4JException {
		
						
		if (part instanceof org.docx4j.openpackaging.parts.JaxbXmlPart) {

			try {				
		        // Add ZIP entry to output stream.
		        out.putNextEntry(new ZipEntry(zipEntryName));		        

		        ((org.docx4j.openpackaging.parts.JaxbXmlPart)part).marshal( out );
		        
		        // Complete the entry
		        out.closeEntry();
				log.info( "PUT SUCCESS: " + zipEntryName);		
		        
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 

		} else if (part instanceof org.docx4j.openpackaging.parts.CustomXmlDataStoragePart) {

				try {				
			        // Add ZIP entry to output stream.
			        out.putNextEntry(new ZipEntry(zipEntryName));		        

			        ((org.docx4j.openpackaging.parts.CustomXmlDataStoragePart)part).getData().writeDocument( out );
			        
			        // Complete the entry
			        out.closeEntry();
					log.info( "PUT SUCCESS: " + zipEntryName);		
			        
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			
//		} else if (part instanceof org.docx4j.openpackaging.parts.Dom4jXmlPart) {
//
//			try {
//		        // Add ZIP entry to output stream.
//		        out.putNextEntry(new ZipEntry(zipEntryName));		        
//		        
//		        // Do things the DOM4J way
//				OutputFormat format = OutputFormat.createPrettyPrint();
//				format.setEncoding("UTF-8");			
//			    XMLWriter writer = new XMLWriter( out, format );
//			    writer.write( ((org.docx4j.openpackaging.parts.Dom4jXmlPart)part).getDocument() );
//		        // Complete the entry
//		        out.closeEntry();
//				log.info( "PUT SUCCESS: " + zipEntryName);		
//			} catch (Exception e ) {
//				e.printStackTrace();
//				throw new Docx4JException("Failed to put " + zipEntryName, e);
//			}		
						
		} else {
			// Shouldn't happen, since ContentTypeManagerImpl should
			// return an instance of one of the above, or throw an
			// Exception.
			
			log.error("PROBLEM - No suitable part found for: " + zipEntryName);
		}		
		
		
	}
	
	protected void deprecatedSaveRawXmlPart(ZipOutputStream out, String partName, Document xml) throws Docx4JException  {

		try {
	        // Add ZIP entry to output stream.
	        out.putNextEntry(new ZipEntry(partName));
	        
	        
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding("UTF-8");			
		    XMLWriter writer = new XMLWriter( out, format );
		    writer.write( xml );
	        // Complete the entry
	        out.closeEntry();
			log.info( "PUT SUCCESS: " + partName);		
		} catch (Exception e ) {
			e.printStackTrace();
			throw new Docx4JException("Failed to put " + partName, e);
		}		
		
	}
	
	/* recursively 
		(i) get each Part listed in the relationships
		(ii) add the Part to the zip file
		(iii) traverse its relationship
	*/
	public void addPartsFromRelationships(ZipOutputStream out,  RelationshipsPart rp )
	 throws Docx4JException {
		
//		for (Iterator it = rp.iterator(); it.hasNext(); ) {
//			Relationship r = (Relationship)it.next();
//			log.info("For Relationship Id=" + r.getId() + " Source is " + r.getSource().getPartName() + ", Target is " + r.getTargetURI() );
		for ( Relationship r : rp.getRelationships().getRelationship() ) {
			
			log.info("For Relationship Id=" + r.getId() 
					+ " Source is " + rp.getSourceP().getPartName() 
					+ ", Target is " + r.getTarget() );
			
//			if (!r.getTargetMode().equals(TargetMode.INTERNAL) ) {
			if (r.getTargetMode() != null
					&& r.getTargetMode().equals("External") ) {
				
				// ie its EXTERNAL
				// As at 1 May 2008, we don't have a Part for these;
				// there is just the relationship.

				log.warn("Encountered external resource " + r.getTarget() 
						   + " of type " + r.getType() );
				
				// So
				continue;				
			}
			
			try {
				//String resolvedPartUri = URIHelper.resolvePartUri(r.getSourceURI(), r.getTargetURI() ).toString();

				String resolvedPartUri = URIHelper.resolvePartUri(rp.getSourceURI(), new URI(r.getTarget() ) ).toString();		
				
				// Now drop leading "/'
				resolvedPartUri = resolvedPartUri.substring(1);				
				
				// Now normalise it .. ie abc/def/../ghi
				// becomes abc/ghi
				// Maybe this isn't necessary with a zip file,
				// - ZipFile class may be smart enough to do it.
				// But it is certainly necessary in the JCR case.
//				target = (new java.net.URI(target)).normalize().toString();
//				log.info("Normalised, it is " + target );				
				
//				Document contents = getDocumentFromZippedPart( zf,  target);
				
				// TODO - if this is already in our hashmap, skip
				// to the next				
				if (!false) {
					log.info("Getting part /" + resolvedPartUri );
					
					Part part = p.getParts().get(new PartName("/" + resolvedPartUri));
					
					if (part==null) {
						log.error("Part " + resolvedPartUri + " not found!");
					} else {
						log.info(part.getClass().getName() );
					}
					
					savePart(out, part);
					
				}
					
			} catch (Exception e) {
				throw new Docx4JException("Failed to add parts from relationships", e);				
			}
		}
		
		
	}


	/**
	 * @param out
	 * @param resolvedPartUri
	 * @param part
	 * @throws Docx4JException
	 * @throws IOException
	 */
	public void savePart(ZipOutputStream out, Part part)
			throws Docx4JException, IOException {
		
		// Drop the leading '/'
		String resolvedPartUri = part.getPartName().getName().substring(1);
		
		if (part instanceof BinaryPart ) {
			log.info(".. saving binary stuff" );
			saveRawBinaryPart( out, part );
			
		} else {
			log.info(".. saving " );					
			saveRawXmlPart( out, part );
		}
		
		// recurse via this parts relationships, if it has any
		if (part.getRelationshipsPart()!= null ) {
			RelationshipsPart rrp = part.getRelationshipsPart();
			log.info("Found relationships " + rrp.getPartName() );
			String relPart = PartName.getRelationshipsPartName(resolvedPartUri);
			log.info("Cf constructed name " + relPart );
			
			//deprecatedSaveRawXmlPart(out, relPart, rrp.getDocument() );
			// 2008 06 12 - try this neater method
			saveRawXmlPart(out, rrp, relPart );
			
			addPartsFromRelationships(out, rrp );
		} else {
			log.info("No relationships for " + resolvedPartUri );					
		}
	}
	
	protected void saveRawBinaryPart(ZipOutputStream out, Part part) throws Docx4JException {

		// Drop the leading '/'
		String resolvedPartUri = part.getPartName().getName().substring(1);

		//InputStream bin = ((BinaryPart)part).getBinaryData();		

		try {
	        // Add ZIP entry to output stream.
	        out.putNextEntry(new ZipEntry(resolvedPartUri));
			
			// Copy the input stream to the output stream
//			byte[] buffer = new byte[8192];
//			int amount;
//			while (( amount = bin.read(buffer)) >=0 ) {
//				log.debug(amount);
//				out.write(buffer, 0, amount);			
//			}
	        
	        java.nio.ByteBuffer bb = ((BinaryPart)part).getBuffer();
	        bb.clear();
	        byte[] bytes = new byte[bb.capacity()];
	        bb.get(bytes, 0, bytes.length);
	        	        
	        out.write( bytes );

			// Complete the entry
	        out.closeEntry();
			
		} catch (Exception e ) {
			throw new Docx4JException("Failed to put binary part", e);			
		}
		
		log.info( "PUT SUCCESS: " + resolvedPartUri);		
		
	}
	
	
	
	
	private void dumpZipFileContents(ZipFile zf) {
		Enumeration entries = zf.entries();
		// Enumerate through the Zip entries until we find the one named
		// '[Content_Types].xml'.
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			log.info( "\n\n" + entry.getName() + "\n" );
			InputStream in = null;
			try {			
				in = zf.getInputStream(entry);
			} catch (IOException e) {
				e.printStackTrace() ;
			}				
			SAXReader xmlReader = new SAXReader();
			Document xmlDoc = null;
			try {
				xmlDoc = xmlReader.read(in);
			} catch (DocumentException e) {
				// Will land here for binary files eg gif file
				e.printStackTrace() ;
			}
			debugPrint(xmlDoc);
			
		}
		
	}
	
	
	private void debugPrint( Document coreDoc) {
		try {
			OutputFormat format = OutputFormat.createPrettyPrint();
		    XMLWriter writer = new XMLWriter( System.out, format );
		    writer.write( coreDoc );
		} catch (Exception e ) {
			e.printStackTrace();
		}	    
	}
		
	
	
}