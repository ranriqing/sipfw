package gcs.modules.attachment.bean;

import gcs.modules.attachment.domain.GcsAttachment;
import gcs.modules.storage.domain.GcsResource;
import lombok.Data;

@Data
public class AttachmentWrapper {
    private GcsAttachment attachment;
    private GcsResource resource;
}
