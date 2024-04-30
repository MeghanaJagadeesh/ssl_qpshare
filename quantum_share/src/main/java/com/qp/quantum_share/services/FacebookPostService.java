package com.qp.quantum_share.services;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;
import org.springframework.web.multipart.MultipartFile;

import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.FacebookUserDao;
import com.qp.quantum_share.dto.FaceBookUser;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.MediaPost;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.exception.FBException;
import com.qp.quantum_share.response.ErrorResponse;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.ResponseWrapper;
import com.qp.quantum_share.response.SuccessResponse;
import com.restfb.BinaryAttachment;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.types.FacebookType;
import com.restfb.types.GraphResponse;
import com.restfb.types.ResumableUploadStartResponse;
import com.restfb.types.ResumableUploadTransferResponse;

@Service
public class FacebookPostService {

	@Autowired
	ConfigurationClass config;

	@Autowired
	FacebookUserDao facebookUserDao;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	SuccessResponse successResponse;

	@Autowired
	ErrorResponse errorResponse;

	public boolean postToPage(String pageId, String pageAccessToken, String message) {

		FacebookClient client = config.getFacebookClient(pageAccessToken);
		try {
			FacebookType response = client.publish(pageId + "/feed", FacebookType.class,
					Parameter.with("message", message));
			System.out.println("Post ID: " + response.getId());
			return true;
		} catch (FacebookException e) {
			System.out.println("Error posting to page: " + e.getMessage());
			return false;
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}

	public ResponseEntity<ResponseWrapper> postMediaToPage(MediaPost mediaPost, MultipartFile mediaFile,
			FaceBookUser user) {
		try {
			List<FacebookPageDetails> pages = user.getPageDetails();
			if (pages.isEmpty()) {
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setMessage("No pages are available for this Facebook account.");
				structure.setPlatform("facebook");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}
			for (FacebookPageDetails page : pages) {

				String facebookPageId = page.getFbPageId();
				String pageAccessToken = page.getFbPageAceessToken();

				FacebookClient client = config.getFacebookClient(pageAccessToken);

				FacebookType response;
				System.out.println("inside try ");
				if (isVideo(mediaFile)) {
					byte[] videoByte = mediaFile.getBytes();
					int videosize = videoByte.length;
					String uploadSessionId = createVideoUploadSession(client, facebookPageId, videosize);
					uploadSessionId = uploadSessionId.replaceAll("\"", "");
					System.out.println("uploadSessionId : " + uploadSessionId);
					long startOffset = 0;

					while (startOffset < videosize) {
						startOffset = uploadVideoChunk(client, facebookPageId, uploadSessionId, startOffset, videoByte);
					}
					GraphResponse finalResponse = finishVideoUploadSession(facebookPageId, client, uploadSessionId,
							mediaPost.getCaption());
					if (finalResponse.isSuccess()) {
						successResponse.setCode(HttpStatus.OK.value());
						successResponse.setMessage("Posted On FaceBook");
						successResponse.setStatus("success");
						successResponse.setPlatform("facebook");
						successResponse.setData(finalResponse);
						return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(successResponse),
								HttpStatus.OK);
					} else {
						errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
						errorResponse.setMessage("Request Failed");
						errorResponse.setStatus("error");
						errorResponse.setPlatform("facebook");
						errorResponse.setData(finalResponse);
						return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(errorResponse),
								HttpStatus.INTERNAL_SERVER_ERROR);
					}

				} else {
					response = client.publish(facebookPageId + "/photos", FacebookType.class,
							BinaryAttachment.with("source", mediaFile.getBytes()),
							Parameter.with("message", mediaPost.getCaption()));
					System.out.println("Post ID: " + response.getId());
					System.out.println("response  " + response);
					if (response.getId() != null) {
						successResponse.setCode(HttpStatus.OK.value());
						successResponse.setMessage("Posted On FaceBook");
						successResponse.setStatus("success");
						successResponse.setData(response);
						successResponse.setPlatform("facebook");
						return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(successResponse),
								HttpStatus.OK);
					} else {
						errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
						errorResponse.setMessage("Request Failed");
						errorResponse.setStatus("error");
						errorResponse.setData(response);
						errorResponse.setPlatform("facebook");
						return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(errorResponse),
								HttpStatus.INTERNAL_SERVER_ERROR);
					}
				}
			}
			return null;

		} catch (FacebookException e) {
			throw new FBException(e.getMessage(), "facebook");
		} catch (IllegalArgumentException e) {
			throw new CommonException(e.getMessage());
		} catch (IOException e) {
			errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			errorResponse.setMessage("IOException occurred");
			errorResponse.setStatus("error");
			errorResponse.setData(null); // You may need to handle this differently
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(errorResponse),
					HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		} catch (InternalServerError error) {
			throw new CommonException(error.getMessage());
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}

	}

	// post video
	public String createVideoUploadSession(FacebookClient client, String pageId, long fileSize) {
		ResumableUploadStartResponse response = client.publish(pageId + "/videos", ResumableUploadStartResponse.class,
				Parameter.with("upload_phase", "start"), Parameter.with("file_size", fileSize));
		return response.getUploadSessionId();
	}

	public Long uploadVideoChunk(FacebookClient client, String facebookPageId, String uploadSessionId, long startOffset,
			byte[] vidFile) {
		ResumableUploadTransferResponse response = client.publish(facebookPageId + "/videos",
				ResumableUploadTransferResponse.class, BinaryAttachment.with("video_file_chunk", vidFile),
				Parameter.with("upload_phase", "transfer"), Parameter.with("start_offset", startOffset),
				Parameter.with("upload_session_id", uploadSessionId));
		return response.getStartOffset();
	}

	public GraphResponse finishVideoUploadSession(String facebookPageId, FacebookClient client, String uploadSessionId,
			String message) {
		GraphResponse response = client.publish(facebookPageId + "/videos", GraphResponse.class,
				Parameter.with("upload_phase", "finish"), Parameter.with("upload_session_id", uploadSessionId),
				Parameter.with("description", message));
		System.out.println("video response " + response);
		return response;
	}

	public boolean isVideo(MultipartFile file) {
		if (file.getContentType().startsWith("video")) {
			return true;
		} else if (file.getContentType().startsWith("image")) {
			return false;
		} else {
			throw new IllegalArgumentException("Unsupported file type: " + file.getContentType());
		}
	}

	
}
