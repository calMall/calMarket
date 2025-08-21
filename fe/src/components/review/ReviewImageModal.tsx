"use client";

import { useEffect, useRef } from "react";
import ModalCover from "../common/ModalCover";
import ContainImage from "../common/ContainImage";

interface props {
  setViewModal: React.Dispatch<React.SetStateAction<boolean>>;
  imageUrl: string;
}

export default function ReviewImageModal({ setViewModal, imageUrl }: props) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        setViewModal(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [ref, setViewModal]);

  return (
    <ModalCover>
      <div className="rt review-big-image" ref={ref}>
        <ContainImage url={imageUrl} alt="review-image" />
      </div>
    </ModalCover>
  );
}
