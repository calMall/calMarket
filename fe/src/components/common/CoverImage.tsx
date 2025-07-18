import Image from "next/image";

export default function CoverImage({ url, alt }: { url: string; alt: string }) {
  return (
    <>
      <Image
        src={url}
        alt={alt}
        style={{ objectFit: "cover" }}
        priority
        fill
        sizes="100%"
      />
    </>
  );
}
