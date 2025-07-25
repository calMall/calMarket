"use client";
import { newImageSizing } from "@/utils/newImageSizing";
import ContainImage from "../common/ContainImage";
import { useState } from "react";

interface props {
  images: string[];
  itemName: string;
}
export default function ProductDetailImage({ images, itemName }: props) {
  const [idx, setIdx] = useState(0);

  return (
    <div>
      <h3 className="m-view mb-1">{itemName}</h3>
      <div className="rt product-detail-big wf" key={idx}>
        <ContainImage
          url={`${newImageSizing(images[idx], 1024)}`}
          alt="product"
        />
      </div>
      <div className="wf flex gap-1 jc ac mt-1">
        {images.map((image, index) => (
          <button
            className="rt product-detail-sm-image"
            key={image}
            onClick={() => setIdx(index)}
          >
            <ContainImage alt="small image" url={image} />
            {idx === index && <div className="outline ab" />}
          </button>
        ))}
      </div>
    </div>
  );
}
