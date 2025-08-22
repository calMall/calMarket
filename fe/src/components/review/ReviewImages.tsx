"use client";
import ContainImage from "../common/ContainImage";
import { useState } from "react";
import ReviewImageModal from "./ReviewImageModal";

export default function ReviewImages({ imageList }: { imageList: string[] }) {
  const [isViewModal, setIsViewModal] = useState(false);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const handleImageClick = (imageUrl: string) => {
    setIsViewModal(true);
    setSelectedImage(imageUrl);
  };

  return (
    <div>
      {imageList && imageList.length > 0 && (
        <div className="review-image-contain mt-1 gap-05 flex ac">
          {imageList.map((url, idx) => (
            <button
              className="review-image rt"
              key={idx}
              onClick={() => handleImageClick(url)}
            >
              <ContainImage url={`${url}`} alt={`review-image-${idx}`} />
            </button>
          ))}
        </div>
      )}
      {isViewModal && selectedImage && (
        <ReviewImageModal
          setViewModal={setIsViewModal}
          imageUrl={selectedImage}
        />
      )}
    </div>
  );
}
