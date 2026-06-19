#pragma once
// MobileGlues - gl/persistent_buffers.h
// Persistent Mapped Buffers — zero-copy CPU→GPU via GL_EXT_buffer_storage
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
#include <cstddef>

namespace MG {

bool hasPersistentBufSupport();

class PersistentBuffer {
public:
    bool  create(GLenum target, size_t bytes);
    void  destroy();
    void* ptr()      const { return m_mapped; }
    bool  isPersist()const { return m_mapped!=nullptr; }
    bool  isValid()  const { return m_buf!=0; }
    GLuint id()      const { return m_buf; }
    void  bind()     const;
    void  upload(size_t off, const void* data, size_t len);
private:
    GLuint m_buf=0; GLenum m_target=GL_ARRAY_BUFFER;
    size_t m_size=0; void* m_mapped=nullptr;
};

} // MG
#endif
